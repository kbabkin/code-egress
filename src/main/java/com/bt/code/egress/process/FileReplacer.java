package com.bt.code.egress.process;

import com.bt.code.egress.Config;
import com.bt.code.egress.read.CsvLineMatcher;
import com.bt.code.egress.read.LineLocation;
import com.bt.code.egress.read.LineToken;
import com.bt.code.egress.write.FileCompleted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class FileReplacer {
    private final LineReplacer lineReplacer;
    private final TextMatched.Listener textMatchedListener;
    private final Config.CsvReplacementConfig csvConfig;
    private final String csvDelim;
    private final String csvQuote;

    public FileCompleted replace(FileLocation file, BufferedReader bufferedReader) throws IOException {
        log.info("Read file: {}", file);
        boolean isCsv = isEligibleForCsvReplacement(file.getFilename());
        if (isCsv) {
            log.info("CSV processing will be applied for {}", file);
        }

        String line;
        int lineNum = 0;
        ArrayList<String> originalLines = new ArrayList<>();
        List<String> replacedLines = new ArrayList<>();
        int lineNumber = 0;
        try {
            CsvLineMatcher csvLineMatcher = null;
            while (((line = bufferedReader.readLine()) != null)) {
                if (isCsv && lineNumber == 0) {
                    csvLineMatcher = new CsvLineMatcher(
                            csvConfig.get(file.getFilename()).getColumns(),
                            line, csvDelim, csvQuote,
                            file.getFilename());
                }
                String replace = lineReplacer.replace(line, new LineLocation(file.toReportedPath(), ++lineNum), textMatchedListener, csvLineMatcher);
                originalLines.add(line);
                replacedLines.add(replace);
                lineNumber++;
            }
        } catch (MalformedInputException e) {
            log.error("Failed to read file {}", file, e);
            textMatchedListener.onMatched(new TextMatched(new LineLocation(file.toReportedPath(), 0),
                    new LineToken(""), null, "", "FAILED to read file " + file));
        }
        return new FileCompleted(file, originalLines, replacedLines);
    }

    private boolean isEligibleForCsvReplacement(String filename) {
        return csvConfig.includes(filename) && csvConfig.getEnabled();
    }
}
