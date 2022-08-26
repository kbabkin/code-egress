package com.bt.code.egress.process;

import com.bt.code.egress.Config;
import com.bt.code.egress.read.CsvLineMatcher;
import com.bt.code.egress.read.LineLocation;
import com.bt.code.egress.read.LineToken;
import com.bt.code.egress.report.Stats;
import com.bt.code.egress.write.FileCompleted;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class FileReplacer {
    private final LineReplacer lineReplacer;
    private final TextMatched.Listener textMatchedListener;
    private final Config.CsvReplacementConfig csvConfig;
    private final String csvDelim;
    private final String csvQuote;
    @Getter
    private final Map<String, Set<String>> errors = Maps.newLinkedHashMap();

    public FileCompleted replace(FileLocation file, BufferedReader bufferedReader) throws IOException {
        log.info("Read file: {}", file);

        boolean isEligibleCsv = isEligibleForCsvReplacement(file.getFilename());
        if (isEligibleCsv) {
            //CSV file with respective column configuration
            log.info("CSV processing will be applied for {}", file);
            Stats.csvFileWithColumnReplacements();
        }

        String line;
        int lineNum = 0;
        ArrayList<String> originalLines = new ArrayList<>();
        List<String> replacedLines = new ArrayList<>();
        CsvLineMatcher csvLineMatcher = null;
        try {
            while (((line = bufferedReader.readLine()) != null)) {
                if (isEligibleCsv && lineNum == 0) {
                    csvLineMatcher = new CsvLineMatcher(
                            csvConfig.get(file.getFilename()).getColumns(),
                            line, csvDelim, csvQuote,
                            file.getFilename());
                }
                String replace = lineReplacer.replace(line, new LineLocation(file.toReportedPath(), ++lineNum), textMatchedListener, csvLineMatcher);
                originalLines.add(line);
                replacedLines.add(replace);
            }
        } catch (Exception e) {
            log.error("Failed to process file {}", file, e);
            Stats.fileFailed();
            textMatchedListener.onMatched(new TextMatched(new LineLocation(file.toReportedPath(), 0),
                    new LineToken(""), null, "", "FAILED to read file " + file));
        }
        if (csvLineMatcher != null && !CollectionUtils.isEmpty(csvLineMatcher.getErrors())) {
            errors.put(file.toReportedPath(), csvLineMatcher.getErrors());
        }
        return new FileCompleted(file, originalLines, replacedLines);
    }

    private boolean isEligibleForCsvReplacement(String filename) {
        return csvConfig.includes(filename) && csvConfig.getEnabled();
    }

    public void verify() {
        if (!errors.isEmpty()) {
            log.info("File errors: ");
            StringBuilder sbErrors = new StringBuilder();
            for (String file : errors.keySet()) {
                sbErrors.append("\n=======================================================\n");
                sbErrors.append(String.format("%d error(s) in %s\n",
                        errors.get(file).size(),
                        file));
                sbErrors.append("=======================================================\n\t");
                sbErrors.append(errors.get(file)
                        .stream()
                        .collect(Collectors.joining("\n\t")));
            }
            log.info(sbErrors.toString());
        }
    }
}
