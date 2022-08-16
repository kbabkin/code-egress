package com.bt.code.egress.process;

import com.bt.code.egress.manual.ReportLine;
import com.bt.code.egress.read.LineToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class Picker {
    static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.withHeader(
            "Correct", "Word", "Context", "File", "Line", "Replacement", "Comment");
    private static final String WRAP = "..";
    private final List<ReportLine> reportLines = new ArrayList<>();
    private int contextLength = 15;
    private String statusFile = "status.csv";

    public void pick(Matched matched) {
        log.info("Matched: {}", matched);
        reportLines.add(new ReportLine(
                null,
                matched.getLineToken().getWordLowerCase(),
                getContext(matched.getLineToken()),
                matched.getLineLocation().getFile(),
                matched.getLineLocation().getLineNum(),
                matched.getReplacement(),
                matched.getComment()
        ));
    }

    private String getContext(LineToken lineToken) {
        String line = lineToken.getLine();

        int startPos = lineToken.getStartPos() - contextLength;
        if (startPos < 0 || (startPos > 0 && startPos - WRAP.length() < 0)) {
            startPos = 0;
        }
        while (startPos < lineToken.getStartPos() - 1 && isWhiteSpace(line.charAt(startPos))) {
            startPos++;
        }

        int endPos = lineToken.getEndPos() + contextLength;
        if (endPos > line.length() || (endPos < line.length() && endPos + WRAP.length() > line.length())) {
            endPos = line.length();
        }
        while (endPos > lineToken.getEndPos() + 1 && isWhiteSpace(line.charAt(endPos - 1))) {
            endPos--;
        }

        return (startPos > 0 ? WRAP : "") +
                line.substring(startPos, endPos) +
                (endPos < line.length() ? WRAP : "");
    }

    static boolean isWhiteSpace(char c) {
        return c <= ' ';
    }

    public void write() {
        log.info("Writing status file: {}", statusFile);
        reportLines.sort(Comparator.nullsLast(Comparator.comparing(ReportLine::getWord).thenComparing(
                Comparator.nullsLast(Comparator.comparing(ReportLine::getContext)))));
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(statusFile))) {
            CSVPrinter printer = new CSVPrinter(writer, CSV_FORMAT);
            for (ReportLine reportLine : reportLines) {
                printer.printRecord(
                        reportLine.getCorrect(),
                        reportLine.getWord(),
                        reportLine.getContext(),
                        reportLine.getFile(),
                        reportLine.getLine(),
                        reportLine.getReplacement(),
                        reportLine.getComment());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write status file " + statusFile, e);
        }

    }
}
