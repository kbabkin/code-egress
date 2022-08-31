package com.bt.code.egress.process;

import com.bt.code.egress.Config;
import com.bt.code.egress.read.LineLocation;
import com.bt.code.egress.read.LineToken;
import com.bt.code.egress.read.ReportMatcher;
import com.bt.code.egress.read.WordMatch;
import com.bt.code.egress.report.ReportHelper;
import com.bt.code.egress.report.Stats;
import com.bt.code.egress.write.FileCompleted;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.text.StringSubstitutor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class CsvFileReplacer {
    private final TextFileReplacer textFileReplacer;
    private final LineReplacer lineReplacer;
    private final ReportMatcher reportMatcher;
    private final ReportHelper reportHelper;
    private final TextMatched.Listener textMatchedListener;
    private final Config.CsvReplacementConfig csvConfig;
    private final String csvDelim;
    private final String csvQuote;

    private Config.CsvFileConfig getCsvFileConfig(String filename) {
        return csvConfig.getEnabled() ? csvConfig.get(filename) : null;
    }

    public FileCompleted replace(FileLocation file, BufferedReader bufferedReader) throws IOException {
        Config.CsvFileConfig csvFileConfig = getCsvFileConfig(file.getFilename());
        if (csvFileConfig != null) {
            return replaceCsv(file, bufferedReader, csvFileConfig);
        } else {
            return textFileReplacer.replace(file, bufferedReader);
        }
    }

    static class GuardedColumns {
        private final boolean[] guarded;
        private final String[] templates;
        private final Map<String, Integer> mapping;

        GuardedColumns(String file, Map<String, String> guardedColumns, Map<String, Integer> headerMap) {
            boolean[] guarded = new boolean[headerMap.size()];
            String[] templates = new String[headerMap.size()];
            Map<String, Integer> mapping = new HashMap<>();
            List<String> headers = new ArrayList<>(headerMap.keySet());//todo?
            for (int i = 0; i < headers.size(); i++) {
                String header = headers.get(i);
                templates[i] = guardedColumns.get(header);
                guarded[i] = templates[i] != null;
                mapping.put(header, i);
            }
            this.guarded = guarded;
            this.templates = templates;
            this.mapping = mapping;

            Set<String> missingColumns = guardedColumns.keySet().stream()
                    .filter(n -> !headerMap.containsKey(n))
                    .collect(Collectors.toSet());
            if (!missingColumns.isEmpty()) {
                Stats.addError(file, "Missing CSV columns: " + missingColumns);
            }
        }

        boolean isGuarded(int index) {
            return guarded[index];
        }

        String getTemplate(int index) {
            return templates[index];
        }

        int size() {
            return guarded.length;
        }

        StringSubstitutor getStringSubstitutor(List<String> values) {
            return new StringSubstitutor(name -> Optional.of(name)
                    .map(mapping::get)
                    .map(values::get)
                    .orElse(null),
                    "{", "}", '$');
        }

    }

    public FileCompleted replaceCsv(FileLocation file, BufferedReader bufferedReader, Config.CsvFileConfig csvFileConfig) throws IOException {
        //CSV file with respective column configuration
        String reportedPath = file.toReportedPath();
        log.info("Process file as CSV: {}", reportedPath);

        List<List<String>> originalRecords = new ArrayList<>();
        List<LineReplacer.MatchParam> firstRunMatches = new ArrayList<>();
        CSVParser recordsParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(bufferedReader);
        Map<String, Integer> headerMap = recordsParser.getHeaderMap();
        GuardedColumns guardedColumns = new GuardedColumns(reportedPath, csvFileConfig.getColumns(), headerMap);
        for (CSVRecord record : recordsParser) {
            originalRecords.add(Lists.newArrayList(record.iterator()));
            LineLocation lineLocation = new LineLocation(reportedPath, (int) record.getRecordNumber());
            for (int i = 0; i < guardedColumns.size() && i < record.size(); i++) {
                if (guardedColumns.isGuarded(i)) {
                    String cell = record.get(i);
                    firstRunMatches.addAll(lineReplacer.getMatchParams(cell, lineLocation));
                }
            }
        }

        Boolean allowed = reportAndGetAllowed(reportedPath, csvFileConfig, firstRunMatches);

        int lineNum = 0;
        List<List<String>> replacedRecords = new ArrayList<>();
        for (List<String> originalRecord : originalRecords) {
            lineNum++; // 0 is header
            List<String> replacedRecord = new ArrayList<>(originalRecord.size());
            LineLocation lineLocation = new LineLocation(reportedPath, lineNum);
            StringSubstitutor csvSubstitutor = guardedColumns.getStringSubstitutor(originalRecord);
            for (int i = 0; i < guardedColumns.size() && i < originalRecord.size(); i++) {
                String cell = originalRecord.get(i);
                String replace = guardedColumns.isGuarded(i)
                        ? (Boolean.TRUE.equals(allowed) ? cell : csvSubstitutor.replace(guardedColumns.getTemplate(i)))
                        : lineReplacer.replace(cell, lineLocation, textMatchedListener);
                replacedRecord.add(replace);
            }
            replacedRecords.add(replacedRecord);
        }
        Stats.csvFileWithColumnReplacements();
        List<String> headers = new ArrayList<>(headerMap.keySet());//todo?
        return new FileCompleted(file,
                write(CSVFormat.DEFAULT, headers, originalRecords),
                write(CSVFormat.DEFAULT, headers, replacedRecords));
    }

    private Boolean reportAndGetAllowed(String reportedPath, Config.CsvFileConfig csvFileConfig, List<LineReplacer.MatchParam> firstRunMatches) {
        String joinedWord = "csv:" + csvFileConfig.getFilename() + ":" + String.join(",", csvFileConfig.getColumns().keySet());
        String joinedReplacement = String.join(",", csvFileConfig.getColumns().values());
        String joinedContext = getJoinedContext(firstRunMatches);
        LineToken joinedLineToken = new JoinedLineToken(joinedWord.toLowerCase(), joinedContext);
        LineLocation joinedLineLocation = new LineLocation(reportedPath, 0);

        Boolean allowed = reportMatcher.getAllowed(joinedLineToken, joinedLineLocation);

        textMatchedListener.onMatched(new TextMatched(joinedLineLocation, joinedLineToken, allowed, joinedReplacement,
                "CSV replace all rows"));
        return allowed;
    }

    List<String> write(CSVFormat csvFormat, List<String> headers, List<List<String>> records) {
        StringWriter writer = new StringWriter();
        String[] header = headers.toArray(new String[0]);
        try (CSVPrinter printer = new CSVPrinter(writer, csvFormat.withHeader(header))) {
            for (List<String> record : records) {
                printer.printRecord(record);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to CSV format", e);
        }
        return Arrays.asList(writer.toString().split("[\\r\\n]+"));
    }

    public String getJoinedContext(List<LineReplacer.MatchParam> firstRunMatches) {
        if (!firstRunMatches.isEmpty()) {
            firstRunMatches = firstRunMatches.stream()
                    .filter(matchParam -> !Boolean.TRUE.equals(matchParam.getAllowed()))
                    .filter(matchParam -> matchParam.getConflict() == null)
                    .collect(Collectors.toList());
        }

        if (firstRunMatches.isEmpty()) {
            return "No guarded words found";
        }

        int limit = 2;
        String collect = firstRunMatches.stream()
                .limit(limit)
                .map(LineReplacer.MatchParam::getWordMatch)
                .map(WordMatch::getLineToken)
                .map(lt -> lt.getContext(reportHelper))
                .collect(Collectors.joining(", "));
        return firstRunMatches.size() > limit ? collect + " and " + (firstRunMatches.size() - limit) + " others" : collect;
    }

    static class JoinedLineToken extends LineToken {
        private final String word;
        private final String context;

        public JoinedLineToken(String word, String context) {
            super("");
            this.word = word.toLowerCase();
            this.context = context;
        }

        @Override
        public String getWordLowerCase() {
            return word;
        }

        @Override
        public String getWord() {
            return word;
        }

        @Override
        public String getContext(ReportHelper reportHelper) {
            return context;
        }
    }
}
