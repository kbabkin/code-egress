package com.bt.code.egress.read;

import com.bt.code.egress.report.Report;
import com.bt.code.egress.report.ReportHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Paths;
import java.util.*;
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
                .distinct()
                .collect(Collectors.groupingBy(Report.ReportLine::getText));
        return new ReportMatcher(reportHelper, rowsByWord);
    }


    public Boolean getAllowed(LineToken lineToken, LineLocation lineLocation) {
        String word = lineToken.getWordLowerCase();
        List<Report.ReportLine> reportLines = rowsByText.get(word);
        if (reportLines == null) {
            return null;
        }

        String tokenContext = reportHelper.getContext(lineToken);
        Optional<Boolean> optionalAllowed = reportLines.stream()
                .filter(r -> {
                    String context = r.getContext();
                    if (StringUtils.isBlank(context)) {
                        return true;
                    }
                    return context.equals(tokenContext);
                })
                .filter(r -> {
                    String file = r.getFile();
                    return StringUtils.isBlank(file) || file.equals(lineLocation.getFile());
                })
                .map(Report.ReportLine::getAllow)
                // if both allowing and disallowing rows, use allowing one
                .reduce((b1, b2) -> true);

        optionalAllowed.ifPresent(allowed -> log.info("Report allowed: {} for {}, {}, {}", allowed, word, lineToken, lineLocation));
        return optionalAllowed.orElse(null);
    }
}
