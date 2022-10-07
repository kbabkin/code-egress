package com.bt.code.egress.process;

import com.bt.code.egress.Config;
import com.bt.code.egress.file.LocalFiles;
import com.bt.code.egress.read.CsvFormatDetector;
import com.bt.code.egress.read.InstructionMatcher;
import com.bt.code.egress.read.LineLocation;
import com.bt.code.egress.read.LineToken;
import com.bt.code.egress.read.WordMatch;
import com.bt.code.egress.report.Report;
import com.bt.code.egress.report.ReportHelper;
import com.bt.code.egress.report.Stats;
import com.bt.code.egress.write.FileCompleted;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.text.StringSubstitutor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

@Slf4j
public class CsvFileReplacer implements FileReplacer {
    private final TextFileReplacer textFileReplacer;
    private final LineReplacer lineReplacer;
    private final InstructionMatcher instructionMatcher;
    private final ReportHelper reportHelper;
    private final TextMatched.Listener textMatchedListener;
    private final Config.CsvReplacementConfig csvConfig;
    private final CSVFormat writeCsvFormat;
    private final CSVFormat readCsvFormat;
    private final CsvFormatDetector csvFormatDetector;
    private final Set<String> templateReplaced = new ConcurrentSkipListSet<>();

    public CsvFileReplacer(TextFileReplacer textFileReplacer, LineReplacer lineReplacer, InstructionMatcher instructionMatcher,
                           ReportHelper reportHelper, TextMatched.Listener textMatchedListener,
                           Config.CsvReplacementConfig csvConfig) {
        this.textFileReplacer = textFileReplacer;
        this.lineReplacer = lineReplacer;
        this.instructionMatcher = instructionMatcher;
        this.reportHelper = reportHelper;
        this.textMatchedListener = textMatchedListener;
        this.csvConfig = csvConfig;
        this.writeCsvFormat = CSVFormat.DEFAULT
                .withDelimiter(csvConfig.getDelim())
                .withQuote(csvConfig.getQuote())
                .withCommentMarker(csvConfig.getCommentMarker());
        this.readCsvFormat = writeCsvFormat.withFirstRecordAsHeader();
        this.csvFormatDetector = new CsvFormatDetector(csvConfig.getDelim(), csvConfig.getQuote(), csvConfig.getCommentMarker());
    }

    private Config.CsvFileConfig getCsvFileConfig(String filename) {
        return csvConfig.isEnabled() ? csvConfig.get(filename) : null;
    }

    @Override
    public FileCompleted replace(FileLocation file, BufferedReader bufferedReader) throws IOException {
        Config.CsvFileConfig csvFileConfig = getCsvFileConfig(file.getFilename());
        if (csvFileConfig != null) {
            return replaceCsv(file, bufferedReader, csvFileConfig);
        } else {
            return textFileReplacer.replace(file, bufferedReader);
        }
    }

    static class GuardedColumns {
        private final String file;
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
            this.file = file;
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
                    .orElseGet(() -> {
                        Stats.addError(file, "Missing CSV columns: " + name);
                        return "_UNRESOLVED_" + name + "_";
                    }),
                    "{", "}", '$');
        }

    }

    public FileCompleted replaceCsv(FileLocation file, BufferedReader bufferedReader, Config.CsvFileConfig csvFileConfig) throws IOException {
        //CSV file with respective column configuration
        String reportedPath = file.toReportedPath();
        log.info("Process file as CSV: {}", reportedPath);

        List<List<String>> originalRecords = new ArrayList<>();
        List<LineReplacer.MatchParam> firstRunMatches = new ArrayList<>();
        CSVParser recordsParser = readCsvFormat.parse(bufferedReader);
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
                boolean guarded = guardedColumns.isGuarded(i);
                if (guarded) {
                    templateReplaced.add(cell.trim().toLowerCase());
                }
                String replace = guarded
                        ? (Boolean.TRUE.equals(allowed) ? cell : csvSubstitutor.replace(guardedColumns.getTemplate(i)))
                        : lineReplacer.replace(cell, lineLocation);
                replacedRecord.add(replace);
            }
            replacedRecords.add(replacedRecord);
        }
        Stats.csvFileWithColumnReplacements();
        List<String> headers = new ArrayList<>(headerMap.keySet());//todo?
        return new FileCompleted(file,
                write(headers, originalRecords, file),
                write(headers, replacedRecords, file));
    }

    private Boolean reportAndGetAllowed(String reportedPath, Config.CsvFileConfig csvFileConfig, List<LineReplacer.MatchParam> firstRunMatches) {
        String joinedWord = "csv:" + csvFileConfig.getFilename() + ":" + String.join(",", csvFileConfig.getColumns().keySet());
        String joinedReplacement = String.join(",", csvFileConfig.getColumns().values());
        String joinedContext = getJoinedContext(firstRunMatches);
        LineToken joinedLineToken = new JoinedLineToken(joinedWord.toLowerCase(), joinedContext);
        LineLocation joinedLineLocation = new LineLocation(reportedPath, 0);

        Report.ReportLine instruction = instructionMatcher.getInstruction(joinedLineToken, joinedLineLocation);
        Boolean allowed = instruction != null ? instruction.getAllow() : null;

        textMatchedListener.onMatched(new TextMatched(joinedLineLocation, joinedLineToken, allowed, joinedReplacement,
                "CSV Column Template"));
        return allowed;
    }

    List<String> write(List<String> headers, List<List<String>> records, FileLocation sourceFile) {
        //Let's not use relativized paths here
        FileLocation originalSourceFile = sourceFile.getOriginalLocation() != null ? sourceFile.getOriginalLocation() : sourceFile;
        StringWriter writer = new StringWriter();
        String[] header = headers.toArray(new String[0]);
        try (CSVPrinter printer = new CSVPrinter(
                writer, writeCsvFormat
                .withHeader(header)
                .withQuoteMode(detectQuoteMode(originalSourceFile)))) {
            for (List<String> record : records) {
                printer.printRecord(record);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to CSV format", e);
        }
        return Arrays.asList(writer.toString().split("[\\r\\n]+"));
    }

    private QuoteMode detectQuoteMode(FileLocation file) throws IOException {
        try {
            return csvFormatDetector.guessQuoteMode(file);
        } catch (IOException ie) {
            throw ie;
        } catch (Throwable t) {
            log.error("Failed to detect quote mode", t);
            return QuoteMode.MINIMAL;
        }
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

    public void saveTemplateReplaced(Path templateReplacedPath) {
        if (templateReplaced.isEmpty()) {
            log.info("No template replaced");
            return;
        }

        log.info("Writing generated replacements to {}", templateReplacedPath);
        try (BufferedWriter writer = LocalFiles.newBufferedWriter(templateReplacedPath)) {
            try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
                for (String w : templateReplaced) {
                    printer.printRecord(w);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write CVS TemplateReplaced to " + templateReplacedPath, e);
        }
        Stats.increment("Words CSV TemplateReplaced", templateReplaced.size());
    }
}
