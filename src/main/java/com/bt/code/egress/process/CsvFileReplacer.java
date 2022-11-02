package com.bt.code.egress.process;

import com.bt.code.egress.Config;
import com.bt.code.egress.file.LocalFiles;
import com.bt.code.egress.read.CsvFormatDetector;
import com.bt.code.egress.read.InstructionMatcher;
import com.bt.code.egress.read.LineLocation;
import com.bt.code.egress.read.LineMatcher;
import com.bt.code.egress.read.LineToken;
import com.bt.code.egress.report.FileErrors;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class CsvFileReplacer implements FileReplacer {
    private final TextFileReplacer textFileReplacer;
    private final LineReplacer lineReplacer;
    private final InstructionMatcher instructionMatcher;
    private final ContextGenerator contextGenerator;
    private final TextMatched.Listener textMatchedListener;
    private final Config.CsvReplacementConfig csvConfig;
    private final boolean skipTemplateReplacement;
    private final CSVFormat writeCsvFormat;
    private final CSVFormat readCsvFormat;
    private final CsvFormatDetector csvFormatDetector;
    private final Map<String, String> dictionaryCandidates = new ConcurrentHashMap<>();

    public CsvFileReplacer(TextFileReplacer textFileReplacer, LineReplacer lineReplacer, InstructionMatcher instructionMatcher,
                           ReportHelper reportHelper, TextMatched.Listener textMatchedListener,
                           Config.CsvReplacementConfig csvConfig, boolean skipTemplateReplacement) {
        this.textFileReplacer = textFileReplacer;
        this.lineReplacer = lineReplacer;
        this.instructionMatcher = instructionMatcher;
        this.contextGenerator = reportHelper.getContextGenerator();
        this.textMatchedListener = textMatchedListener;
        this.csvConfig = csvConfig;
        this.skipTemplateReplacement = skipTemplateReplacement;
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
                FileErrors.addError(file, "Missing CSV columns: " + missingColumns);
            }
        }

        boolean isGuarded(int index) {
            return index < guarded.length && guarded[index];
        }

        String getTemplate(int index) {
            return templates[index];
        }

        StringSubstitutor getStringSubstitutor(List<String> values) {
            return new StringSubstitutor(name -> Optional.of(name)
                    .map(mapping::get)
                    .map(values::get)
                    .orElseGet(() -> {
                        FileErrors.addError(file, "Missing CSV columns: " + name);
                        return "_UNRESOLVED_" + name + "_";
                    }),
                    "{", "}", '$');
        }

    }

    static class ColumnContextGenerators {
        private final ContextGenerator defaultContextGenerator;
        private final List<ContextGenerator> contextGenerators;

        ColumnContextGenerators(ContextGenerator defaultContextGenerator, Map<String, Integer> headerMap) {
            this.defaultContextGenerator = defaultContextGenerator;
            // later used by column index
            this.contextGenerators = headerMap.keySet().stream()
                    .map(header -> (ContextGenerator) (lt -> header + ":" + defaultContextGenerator.getContext(lt)))
                    .collect(Collectors.toList());
        }

        ContextGenerator getContextGenerator(int index) {
            return index < contextGenerators.size() ? contextGenerators.get(index) : defaultContextGenerator;
        }
    }

    public FileCompleted replaceCsv(FileLocation file, BufferedReader bufferedReader, Config.CsvFileConfig csvFileConfig) throws IOException {
        //CSV file with respective column configuration
        String reportedPath = file.toReportedPath();
        log.info("Process file as CSV: {}", reportedPath);

        List<List<String>> originalRecords = new ArrayList<>();
        List<String> firstRunMatches = new ArrayList<>();
        CSVParser recordsParser = readCsvFormat.parse(bufferedReader);
        Map<String, Integer> headerMap = recordsParser.getHeaderMap();
        GuardedColumns dictionaryColumns = new GuardedColumns(reportedPath, csvFileConfig.getDictionary(), headerMap);
        GuardedColumns templateColumns = new GuardedColumns(reportedPath,
                skipTemplateReplacement ? Collections.emptyMap() : csvFileConfig.getColumns(), headerMap);
        ColumnContextGenerators columnContextGenerators = new ColumnContextGenerators(contextGenerator, headerMap);
        for (CSVRecord record : recordsParser) {
            originalRecords.add(Lists.newArrayList(record.iterator()));
            LineLocation lineLocation = new LineLocation(reportedPath, (int) record.getRecordNumber());
            for (int i = 0; i < record.size(); i++) {
//                if (dictionaryColumns.isGuarded(i) || templateColumns.isGuarded(i)) {
                if (templateColumns.isGuarded(i)) {
                    String cell = record.get(i);
                    ContextGenerator columnContextGenerator = columnContextGenerators.getContextGenerator(i);
                    List<LineReplacer.MatchParam> matchParams = lineReplacer.getMatchParams(cell, lineLocation, columnContextGenerator);
                    matchParams.stream()
                            .filter(matchParam -> !Boolean.TRUE.equals(matchParam.getAllowed()))
                            .filter(matchParam -> matchParam.getConflict() == null)
                            .forEach(matchParam -> firstRunMatches.add(
                                    columnContextGenerator.getContext(matchParam.getWordMatch().getLineToken())));
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
            StringSubstitutor templateSubstitutor = templateColumns.getStringSubstitutor(originalRecord);
            for (int i = 0; i < originalRecord.size(); i++) {
                String cell = originalRecord.get(i);
                String replace;
                if (templateColumns.isGuarded(i)) {
                    String mashed = templateSubstitutor.replace(templateColumns.getTemplate(i));
                    if (Boolean.TRUE.equals(allowed)) {
                        replace = cell;
                        if (!cell.equals(mashed)) {
                            dictionaryCandidates.put(cell.trim().toLowerCase(), "template");
                        }
                    } else {
                        replace = mashed;
                    }
                } else {
                    replace = lineReplacer.replace(cell, lineLocation, columnContextGenerators.getContextGenerator(i));
                    if (dictionaryColumns.isGuarded(i) && cell.equals(replace)) {
                        dictionaryCandidates.put(cell.trim().toLowerCase(), "dictionary");
                    }
                }
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

    private Boolean reportAndGetAllowed(String reportedPath, Config.CsvFileConfig csvFileConfig, List<String> firstRunMatches) {
        String joinedWord = "csv:" + csvFileConfig.getFilename() + ":" + String.join(",", csvFileConfig.getColumns().keySet());
        String joinedReplacement = String.join(",", csvFileConfig.getColumns().values());
        String joinedContext = getJoinedContext(firstRunMatches);
        LineToken joinedLineToken = new LineToken(joinedWord.toLowerCase(), 0, joinedWord.length());
        LineLocation joinedLineLocation = new LineLocation(reportedPath, 0);

        Report.ReportLine instruction = instructionMatcher.getInstruction(joinedLineLocation, joinedLineToken, joinedContext);
        Boolean allowed = instruction != null ? instruction.getAllow() : null;

        textMatchedListener.onMatched(new TextMatched(joinedLineLocation, joinedLineToken, allowed,
                joinedContext, joinedReplacement, "CSV Column Template"));
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

    public String getJoinedContext(List<String> firstRunMatches) {
        if (firstRunMatches.isEmpty()) {
            return "No guarded words found";
        }

        int limit = 2;
        String collect = firstRunMatches.stream()
                .limit(limit)
                .collect(Collectors.joining(", "));
        return firstRunMatches.size() > limit ? collect + " and " + (firstRunMatches.size() - limit) + " others" : collect;
    }

    public void saveDictionaryCandidates(Path dictionaryCandidatePath, LineMatcher restoreLineMatcher) {
        if (dictionaryCandidates.isEmpty()) {
            log.info("No CVS Dictionary Candidates");
            return;
        }

        log.info("Writing CVS Dictionary Candidates to {}", dictionaryCandidatePath);
        CSVFormat format = CSVFormat.DEFAULT.withHeader("Text", "Replacement", "Scope", "Comment");
        try (BufferedWriter writer = LocalFiles.newBufferedWriter(dictionaryCandidatePath)) {
            try (CSVPrinter printer = new CSVPrinter(writer, format)) {
                dictionaryCandidates.entrySet().stream()
                        .map(entry -> {
                            String text = entry.getKey();
                            return new String[]{text,
                                    // leave second column empty if report line copy-pasted to word-guard-value.csv
                                    // where it is used as is replacement template
                                    null,
                                    // is it from template or dictionary column
                                    entry.getValue(),
                                    // tricky: restore config is used during replace
                                    !restoreLineMatcher.getMatches(text).isEmpty() ? "Ignore: restorable"
                                            : text.length() < 3 ? "Ignore: too short" : null};
                        })
                        .sorted(Comparator.comparing((String[] r) -> r[3], Comparator.comparingInt(s -> s == null ? 1 : 0))
                                .thenComparing((String[] r) -> r[0], Comparator.comparingInt(s -> s == null || s.length() < 3 ? 0 : 1))
                                .thenComparing((String[] r) -> r[2])
                                .thenComparing((String[] r) -> r[0]))
                        .forEach(e -> {
                            try {
                                printer.printRecord((Object[]) e);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        });
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write CVS Dictionary Candidate to " + dictionaryCandidatePath, e);
        }
        Stats.increment("Words CSV Dictionary Candidate", dictionaryCandidates.size());
    }
}
