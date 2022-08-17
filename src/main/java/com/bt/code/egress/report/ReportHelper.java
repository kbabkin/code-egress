package com.bt.code.egress.report;

import com.bt.code.egress.read.LineToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class ReportHelper {
    enum Headers {
        Allow,
        Word,
        Context,
        File,
        Line,
        Replacement,
        Comment
    }

    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.withHeader(Headers.class);
    private static final String WRAP = "..";
    private final int contextLength;

    public String getContext(LineToken lineToken) {
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

    public void write(Appendable writer, List<Report.ReportLine> reportLines) throws IOException {
        try (CSVPrinter printer = new CSVPrinter(writer, CSV_FORMAT)) {
            for (Report.ReportLine reportLine : reportLines) {
                printer.printRecord(
                        reportLine.getAllow(),
                        reportLine.getWord(),
                        reportLine.getContext(),
                        reportLine.getFile(),
                        reportLine.getLine(),
                        reportLine.getReplacement(),
                        reportLine.getComment());
            }
        }
    }

    public List<Report.ReportLine> read(Path path) {
        if (!Files.exists(path)) {
            log.info("Skip missing allow config report file {}", path);
            return Collections.emptyList();
        }
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            return read(reader);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read allow config from report file " + path, e);
        }
    }

    public List<Report.ReportLine> read(Reader reader) throws IOException {
        List<Report.ReportLine> reportLines = new ArrayList<>();
        CSVParser records = CSV_FORMAT.withFirstRecordAsHeader().parse(reader);
        for (CSVRecord record : records) {
            reportLines.add(new Report.ReportLine(
                    toBoolean(record.get(Headers.Allow)),
                    record.get(Headers.Word),
                    record.get(Headers.Context),
                    record.get(Headers.File),
                    toInteger(record.get(Headers.Line)),
                    record.get(Headers.Replacement),
                    record.get(Headers.Comment)));
        }
        return reportLines;
    }

    static Boolean toBoolean(String str) {
        if (StringUtils.isBlank(str)) {
            return null;
        }
        return str.equalsIgnoreCase("y") || str.equalsIgnoreCase("true")
                || str.equalsIgnoreCase("yes") || str.equals("1");
    }

    static Integer toInteger(String str) {
        if (StringUtils.isBlank(str)) {
            return null;
        }
        return Integer.parseInt(str);
    }
}
