package com.bt.code.egress.read;

import com.bt.code.egress.report.Report;
import com.bt.code.egress.report.ReportHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Paths;
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
public class ReportMatcher {
    private final ReportHelper reportHelper;
    private final Map<String, List<Report.ReportLine>> rowsByText;

    public static ReportMatcher fromConfigs(ReportHelper reportHelper, Set<String> allowReportFiles) {
        Map<String, List<Report.ReportLine>> rowsByWord = allowReportFiles.stream()
                .map(file -> reportHelper.read(Paths.get(file)))
                .flatMap(Collection::stream)
                .filter(r -> Objects.nonNull(r.getAllow()))
                .map(rl -> new Report.ReportLine(rl.getAllow(), rl.getText().toLowerCase(), rl.getContext().toLowerCase(),
                        rl.getFile(), rl.getLine(), rl.getReplacement(), rl.getComment()))
                .distinct()
                .collect(Collectors.groupingBy(Report.ReportLine::getText));
        // longer context has higher priority
        Comparator<String> longerFirst = Comparator.nullsLast(Comparator.comparingInt(String::length).reversed());
        rowsByWord.values().forEach(list -> list.sort(Comparator.comparing(Report.ReportLine::getContext, longerFirst)
                .thenComparing(Report.ReportLine::getFile, longerFirst)));
        return new ReportMatcher(reportHelper, rowsByWord);
    }


    public Boolean getAllowed(LineToken lineToken, LineLocation lineLocation) {
        String word = lineToken.getWordLowerCase();
        String wordContext = lineToken.getContext(reportHelper);
        List<Report.ReportLine> reportLines = rowsByText.get(word);
        if (reportLines == null || reportLines.isEmpty()) {
            return null;
        }

        String tokenContext = wordContext.toLowerCase();
        Optional<Boolean> optionalAllowed = reportLines.stream()
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
                .map(Report.ReportLine::getAllow)
                .filter(Objects::nonNull)
                // first match has longer context
                .findFirst();

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
}
