package com.bt.code.egress.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class ReportWriter implements Report.Listener {
    private static final Comparator<Report.ReportLine> WRITE_ORDER =
            Comparator.comparing(Report.ReportLine::getAllow, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Report.ReportLine::getText, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Report.ReportLine::getContext, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Report.ReportLine::getFile, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Report.ReportLine::getLine, Comparator.nullsLast(Comparator.naturalOrder()));
    private final ReportHelper reportHelper;
    private final Path reportFile;

    @Override
    public void onReport(Report report) {
        log.info("Writing status file: {}", reportFile);
        List<Report.ReportLine> reportLines = new ArrayList<>(report.getReportLines());
        reportLines.sort(WRITE_ORDER);
        try (BufferedWriter writer = Files.newBufferedWriter(reportFile)) {
            reportHelper.write(writer, reportLines);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write status file " + reportFile, e);
        }
    }
}
