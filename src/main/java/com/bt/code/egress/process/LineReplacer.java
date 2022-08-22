package com.bt.code.egress.process;

import com.bt.code.egress.read.LineGuardIgnoreMatcher;
import com.bt.code.egress.read.LineLocation;
import com.bt.code.egress.read.LineToken;
import com.bt.code.egress.read.ReportMatcher;
import com.bt.code.egress.read.WordMatch;
import com.bt.code.egress.report.Stats;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class LineReplacer {

    private final LineGuardIgnoreMatcher lineMatcher;
    private final ReportMatcher reportMatcher;
    private final WordReplacer wordReplacer;

    @Data
    @AllArgsConstructor
    static class MatchParam {
        WordMatch wordMatch;
        Boolean allowed;
        WordMatch conflict;
    }

    public String replace(String line, LineLocation lineLocation, TextMatched.Listener textMatchedListener) {
        List<WordMatch> rawMatches = lineMatcher.getMatches(line);
        if (rawMatches.isEmpty()) {
            return line;
        }
        Stats.wordsMatched(rawMatches.size());

        List<MatchParam> matchParams = rawMatches.stream()
                // reportMatcher can return null
                .map(rm -> new MatchParam(rm, reportMatcher.getAllowed(rm.getLineToken(), lineLocation), null))
                .collect(Collectors.toList());

        markConflicts(matchParams);

        // sort by position in line
        matchParams.sort(Comparator.comparing(m -> m.getWordMatch().getLineToken().getStartPos()));

        String processed = null;
        int processedPos = 0;
        for (MatchParam matchParam : matchParams) {
            WordMatch wordMatch = matchParam.getWordMatch();
            LineToken lineToken = wordMatch.getLineToken();

            String replacement = wordReplacer.replace(wordMatch);
            String comment = wordMatch.getReason();
            if (Boolean.TRUE.equals(matchParam.getAllowed())) {
                comment = "Allowed, " + wordMatch.getReason() + ", Suggested " + replacement;
                replacement = null;
            } else if (matchParam.getConflict() != null) {
                comment = "CONFLICT with " + matchParam.getConflict().getReason() + ", " + wordMatch.getReason() +
                        ", Suggested " + replacement;
                replacement = null;
            } else {
                String withBefore = line.substring(processedPos, lineToken.getStartPos()) + replacement;
                processed = processed == null ? withBefore : processed + withBefore;
                processedPos = lineToken.getEndPos();
                Stats.wordReplaced();
            }

            textMatchedListener.onMatched(new TextMatched(lineLocation, lineToken, matchParam.getAllowed(), replacement,
                    comment));
        }
        return processed == null ? line : processed + line.substring(processedPos);
    }

    /**
     * Mark overlapped matches as conflicts, modify list inplace.
     */
    static void markConflicts(List<MatchParam> matchParams) {
        if (matchParams.size() <= 1) {
            return;
        }

        // longer matches - higher priority
        matchParams.sort(Comparator.<MatchParam, Integer>comparing(m -> m.getWordMatch().getLineToken().getLength()).reversed());

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
