package com.bt.code.egress.read;

import com.bt.code.egress.process.RestoreWordReplacementGenerator;
import com.bt.code.egress.process.WordReplacementGenerator;
import com.bt.code.egress.report.Report;
import com.bt.code.egress.report.ReportHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class InstructionMatcher {
    private final ReportHelper reportHelper;
    private final Map<String, List<Report.ReportLine>> rowsByText;
    private final Map<String, Set<String>> replacementsByWord;

    public static InstructionMatcher fromConfigs(ReportHelper reportHelper, Set<File> instructionFiles) {
        Map<String, List<Report.ReportLine>> rowsByWord = instructionFiles.stream()
                .map(file -> reportHelper.read(file.toPath()))
                .flatMap(Collection::stream)
//                .filter(r -> Objects.nonNull(r.getAllow()))
                .map(rl -> new Report.ReportLine(rl.getAllow(), rl.getText().toLowerCase(), rl.getContext().toLowerCase(),
                        rl.getFile(), rl.getLine(), rl.getReplacement(), rl.getComment()))
                .distinct()
                .collect(Collectors.groupingBy(Report.ReportLine::getText));
        // longer context has higher priority
        Comparator<String> longerFirst = Comparator.nullsLast(Comparator.comparingInt(String::length).reversed());
        rowsByWord.values().forEach(list -> list.sort(Comparator.comparing(Report.ReportLine::getContext, longerFirst)
                .thenComparing(Report.ReportLine::getFile, longerFirst)));

        Map<String, Set<String>> replacementsByWord = instructionFiles.stream()
                .map(file -> reportHelper.read(file.toPath()))
                .flatMap(Collection::stream)
                .filter(r -> StringUtils.isNotBlank(r.getText()))
                .filter(r -> StringUtils.isNotBlank((r.getReplacement())))
                .collect(Collectors.groupingBy(r -> r.getText().toLowerCase(),
                        Collectors.mapping(Report.ReportLine::getReplacement, Collectors.toSet())));
        return new InstructionMatcher(reportHelper, rowsByWord, replacementsByWord);
    }


    public Report.ReportLine getInstruction(LineToken lineToken, LineLocation lineLocation) {
        String word = lineToken.getWordLowerCase();
        String wordContext = lineToken.getContext(reportHelper);
        List<Report.ReportLine> reportLines = rowsByText.get(word);
        if (reportLines == null || reportLines.isEmpty()) {
            return null;
        }

        String tokenContext = wordContext.toLowerCase();
        Optional<Report.ReportLine> optionalAllowed = reportLines.stream()
                .filter(r -> {
                    String context = r.getContext();
                    return StringUtils.isBlank(context) || context.equals(tokenContext)
                            // ability to provide only part of context
                            || tokenContext.length() > context.length() && context.length() >= word.length() + reportHelper.getContextMinCompareLength()
                            && tokenContext.contains(context) && context.contains(word);
                })
                .filter(r -> {
                    String file = r.getFile();
                    return StringUtils.isBlank(file) || file.equals(lineLocation.getFile())
                            // ability to use file path filter
                            || FilePathMatcher.match(file, lineLocation.getFile());
                })
                // first match has longer context - order from fromConfigs
                .min(Comparator.comparingInt(r ->
                        // exact line num match has priority
                        StringUtils.isNotBlank(r.getFile()) && r.getLine() != null
                                && r.getLine().equals(lineLocation.getLineNum()) ? 0 : 1));

        optionalAllowed.ifPresent(allowed -> log.info("Report allowed: {} for {}, {}, {}", allowed, word, wordContext, lineLocation));
        return optionalAllowed.orElse(null);
    }

    public FilePathMatcher getAllowFilePathMatcher() {
        List<Report.ReportLine> noWord = rowsByText.get("");
        Set<String> fileFilters = noWord == null ? Collections.emptySet() : noWord.stream()
                .map(Report.ReportLine::getFile)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return new FilePathMatcher(fileFilters, Collections.emptySet());
    }

    public Map<String, String> getSimpleReplacements() {
        return replacementsByWord.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size() == 1
                        ? e.getValue().iterator().next() : ""));
    }

    public WordReplacementGenerator getRestoreWordReplacer() {
        return new RestoreWordReplacementGenerator(replacementsByWord);
    }
}
