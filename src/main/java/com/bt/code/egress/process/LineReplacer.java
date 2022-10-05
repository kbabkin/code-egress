package com.bt.code.egress.process;

import com.bt.code.egress.read.InstructionMatcher;
import com.bt.code.egress.read.LineGuardIgnoreMatcher;
import com.bt.code.egress.read.LineLocation;
import com.bt.code.egress.read.LineToken;
import com.bt.code.egress.read.WordMatch;
import com.bt.code.egress.report.Report;
import com.bt.code.egress.report.Stats;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class LineReplacer {

    private final LineGuardIgnoreMatcher lineMatcher;
    private final TextMatched.Listener textMatchedListener;
    private final TextMatched.Listener restoreInstructionDraftListener;
    private final InstructionMatcher instructionMatcher;
    private final WordReplacementGenerator wordReplacementGenerator;

    @Data
    @AllArgsConstructor
    static class MatchParam {
        WordMatch wordMatch;
        Report.ReportLine instruction;
        WordMatch conflict;

        Boolean getAllowed() {
            return instruction != null ? instruction.getAllow() : null;
        }

        String getInstructionReplacement() {
            return instruction != null ? instruction.getReplacement() : null;
        }

        String getInstructionComment() {
            return instruction != null ? instruction.getComment() : null;
        }
    }

    @Data
    @AllArgsConstructor
    static class BackMatch {
        WordMatch wordMatch;
        String replacement;
        int startPos;

        public int getEndPos() {
            return startPos + replacement.length();
        }
    }

    public List<MatchParam> getMatchParams(String line, LineLocation lineLocation) {
        List<WordMatch> matches = lineMatcher.getMatches(line);
        if (matches.isEmpty()) {
            return Collections.emptyList();
        }

        List<MatchParam> matchParams = matches.stream()
                // instructionMatcher can return null
                .map(rm -> new MatchParam(rm, instructionMatcher.getInstruction(rm.getLineToken(), lineLocation), null))
                .collect(Collectors.toList());

        markConflicts(matchParams);
        return matchParams;
    }

    public String replace(String line, LineLocation lineLocation) {
        if (line.length() == 0) {
            return line;
        }
        List<MatchParam> matchParams = getMatchParams(line, lineLocation);
        if (matchParams.isEmpty()) {
            return line;
        }

        // sort by position in line
        matchParams.sort(Comparator.comparing(m -> m.getWordMatch().getLineToken().getStartPos()));

        String processed = null;
        int processedPos = 0;
        List<BackMatch> backMatches = new ArrayList<>();
        for (MatchParam matchParam : matchParams) {
            WordMatch wordMatch = matchParam.getWordMatch();
            LineToken lineToken = wordMatch.getLineToken();

            String replacement;
            String comment;
            if (StringUtils.isNotBlank(matchParam.getWordMatch().getReplacement())) {
                // wordMatch.replacement - from CSV template
                replacement = matchParam.getWordMatch().getReplacement();
                comment = wordMatch.getReason();
            } else if (StringUtils.isNotBlank(matchParam.getInstructionReplacement())) {
                // matchParam.instructionReplacement - from instruction
                replacement = matchParam.getInstructionReplacement();
                comment = "Instruction " + matchParam.getInstructionComment();
            } else {
                // wordMatch.template - default without context
                replacement = wordReplacementGenerator.replace(wordMatch);
                comment = "Generated for " + wordMatch.getReason();
            }

            if (Boolean.TRUE.equals(matchParam.getAllowed())) {
                comment = "Allowed: " + wordMatch.getReason() + ", Suggested: " + replacement;
                replacement = null;
            } else if (matchParam.getConflict() != null) {
                if (!Objects.equals(wordMatch.getLineToken(), matchParam.getConflict().getLineToken())) {
                    log.info("CONFLICT: {}->{} ({}) IS OVERRIDDEN BY {} ({})", wordMatch.getLineToken().getWord(), replacement, wordMatch.getReason(),
                            matchParam.getConflict().getLineToken().getWord(), matchParam.getConflict().getReason());
                    Stats.wordConflict();
                }
                continue;
            } else {
                backMatches.add(new BackMatch(wordMatch, replacement, (processed == null ? 0 : processed.length()) + (lineToken.getStartPos() - processedPos)));
                String withBefore = line.substring(processedPos, lineToken.getStartPos()) + replacement;
                processed = processed == null ? withBefore : processed + withBefore;
                processedPos = lineToken.getEndPos();
                Stats.wordReplaced();
            }

            textMatchedListener.onMatched(new TextMatched(lineLocation, lineToken, matchParam.getAllowed(), replacement,
                    comment));
        }
        String replacedLine = processed == null ? line : processed + line.substring(processedPos);
        if (restoreInstructionDraftListener != null) {
            for (BackMatch backMatch : backMatches) {
                WordMatch wordMatch = backMatch.getWordMatch();
                restoreInstructionDraftListener.onMatched(new TextMatched(lineLocation,
                        new LineToken(replacedLine, backMatch.getStartPos(), backMatch.getEndPos()),
                        null, wordMatch.getLineToken().getWord(), "Restore " + wordMatch.getReason()));
            }
        }
        Stats.wordsMatched(matchParams.size());
        return replacedLine;
    }

    /**
     * Mark overlapped matches as conflicts, modify list inplace.
     */
    static void markConflicts(List<MatchParam> matchParams) {
        if (matchParams.size() <= 1) {
            return;
        }

        // longer matches - higher priority, for same length - exact replacement is better than template
        matchParams.sort(Comparator.<MatchParam, Integer>comparing(m -> m.getWordMatch().getLineToken().getLength()).reversed()
                .thenComparing(mp -> StringUtils.isBlank(mp.getWordMatch().getReplacement()) ? 1 : 0)
                .thenComparing(mp -> StringUtils.isBlank(mp.getWordMatch().getTemplate()) ? 2 :
                        mp.getWordMatch().getTemplate().contains("{") ? 1 : 0));

        // temp store of included matches
        List<MatchParam> toReplace = new ArrayList<>();
        matchParams.stream()
                .peek(m1 -> m1.setConflict(toReplace.stream()
                        .filter(m2 -> !m2.getWordMatch().isCompatible(m1.getWordMatch()))
                        .findFirst()
                        .map(MatchParam::getWordMatch)
                        .orElse(null)))
                .filter(m1 -> !Boolean.TRUE.equals(m1.getAllowed()))
                .forEach(toReplace::add);
    }
}
