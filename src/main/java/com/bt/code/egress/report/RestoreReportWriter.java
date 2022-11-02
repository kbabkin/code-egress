package com.bt.code.egress.report;

import com.google.common.collect.Streams;
import lombok.Value;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RestoreReportWriter extends ReportWriter {
    private final List<Report.ReportLine> prevRestoreInstructions;
    private final String reportName;

    public RestoreReportWriter(ReportHelper reportHelper, Path reportFile, String reportName, List<Report.ReportLine> prevRestoreInstructions) {
        super(reportHelper, reportFile);
        this.prevRestoreInstructions = prevRestoreInstructions;
        this.reportName = reportName;
    }

    public static RestoreReportWriter fromCumulativeConfig(ReportHelper reportHelper, Path reportFile, String reportName, Collection<File> instructionFiles) {
        List<Report.ReportLine> prevRestoreInstructions = instructionFiles.stream()
                .map(file -> reportHelper.read(file.toPath()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        return new RestoreReportWriter(reportHelper, reportFile, reportName, prevRestoreInstructions);
    }

    @Override
    public void onReport(Report report) {
        List<Report.ReportLine> lastReportLines = new ArrayList<>(report.getReportLines());

        Stream<Report.ReportLine> lineStream = Streams.concat(
                lastReportLines.stream(), filterPrevInstructions(lastReportLines));
        Map<RestoreLineKey, Report.ReportLine> reportLineMap = lineStream
                .collect(Collectors.toMap(RestoreLineKey::fromReportLine, Function.identity(),
                        BinaryOperator.minBy(Comparator.comparing(Report.ReportLine::getAllow, ReportHelper.ALLOW_COMPARATOR)
                                .thenComparing(Report.ReportLine::getComment, ReportHelper.CONTEXT_COMPARATOR))));
        List<Report.ReportLine> reportLines = new ArrayList<>(reportLineMap.values());

        sortAndWrite(reportLines);
        Stats.increment("Restore Report Lines - " + reportName, reportLines.size());
    }

    private Stream<Report.ReportLine> filterPrevInstructions(List<Report.ReportLine> lastReportLines) {
        return prevRestoreInstructions.stream();
// throw away prev instructions for replaced files?
//        Set<String> replacedFiles = lastReportLines.stream()
//                .map(Report.ReportLine::getFile)
//                .filter(Objects::nonNull)
//                .collect(Collectors.toSet());
//
//        return prevRestoreInstructions.stream()
//                .filter(r -> !replacedFiles.contains(r.getFile()));
    }

    @Value
    static class RestoreLineKey {
        String text;
        String context;
        String file;
        Integer line;
        String replacement;

        static RestoreLineKey fromReportLine(Report.ReportLine reportLine) {
            return new RestoreLineKey(reportLine.getText(), reportLine.getContext(),
                    reportLine.getFile(), reportLine.getLine(), reportLine.getReplacement());
        }
    }
}
