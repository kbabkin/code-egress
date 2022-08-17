package com.bt.code.egress.report;

import com.bt.code.egress.process.Matched;
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
public class ReportWriter {
    private static final Comparator<ReportLine> WRITE_ORDER =
            Comparator.comparing(ReportLine::getAllow, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(ReportLine::getWord, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(ReportLine::getContext, Comparator.nullsLast(Comparator.naturalOrder()));
    private final ReportHelper reportHelper;
    private final Path reportFile;
    private final List<ReportLine> reportLines = new ArrayList<>();

    public void pick(Matched matched) {
        log.info("Matched: {}", matched);
        reportLines.add(new ReportLine(
                matched.getAllowed(),
                matched.getLineToken().getWordLowerCase(),
                reportHelper.getContext(matched.getLineToken()),
                matched.getLineLocation().getFile(),
                matched.getLineLocation().getLineNum(),
                matched.getReplacement(),
                matched.getComment()
        ));
    }

    public void write() {
        log.info("Writing status file: {}", reportFile);
        reportLines.sort(WRITE_ORDER);
        try (BufferedWriter writer = Files.newBufferedWriter(reportFile)) {
            reportHelper.write(writer, reportLines);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write status file " + reportFile, e);
        }

    }
}
