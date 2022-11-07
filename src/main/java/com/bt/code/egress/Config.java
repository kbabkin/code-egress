package com.bt.code.egress;

import com.bt.code.egress.file.LocalFiles;
import com.bt.code.egress.read.FilePathMatcher;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Configuration
@EnableConfigurationProperties
@ConfigurationProperties
@Data
@Slf4j
public class Config {
    ScanConfig scan = new ScanConfig();
    ReadConfig read = new ReadConfig();
    WriteConfig write = new WriteConfig();
    ContextConfig context = new ContextConfig();
    DirectionConfig replace = new DirectionConfig();
    DirectionConfig restore = new DirectionConfig();
    CsvReplacementConfig csv = new CsvReplacementConfig();
    CopyChangesConfig copy = new CopyChangesConfig();

    public enum ScanDirection {
        REPLACE,
        RESTORE
    }

    @Data
    public static class ScanConfig {
        String project;
        String config;
        String direction;

        ScanDirection getScanMode() {
            if (StringUtils.isBlank(direction)) {
                return ScanDirection.REPLACE;
            }
            return ScanDirection.valueOf(direction.toUpperCase());
        }
    }

    @Data
    public static class ReadConfig {
        // Path(".") was resolved as target/classes
        File folder;
        int threads = 10;
    }

    @Data
    public static class WriteConfig {
        boolean inplace = false;
        File folder;
    }

    @Data
    public static class ContextConfig {
        int keepLength = 15;
        int minCompareLength = 1;
    }

    @Getter
    @Setter
    public static class DirectionConfig {
        MatchingSets file = new Config.MatchingSets();
        MatchingMaps word = new Config.MatchingMaps();
        InstructionConfig instruction = new InstructionConfig();
        String defaultTemplate;
        File report;
        File restoreInstructionCumulative;
        File restoreInstructionLast;
        File generatedReplacement;
        File fileError;
    }

    public Config.DirectionConfig getDirectionConfig() {
        return ScanDirection.RESTORE.equals(getScan().getScanMode()) ? getRestore() : getReplace();
    }

    @Data
    public static class MatchingSets {
        MatchingSet guard = new MatchingSet();
        MatchingSet ignore = new MatchingSet();
    }

    @Data
    public static class MatchingSet {
        Set<String> values = new HashSet<>();
        Set<String> patterns = new HashSet<>();
        Set<String> valueFiles = new HashSet<>();
        Set<String> patternFiles = new HashSet<>();

        @Value
        public static class ValuesAndPatternsSet {
            Set<String> values;
            Set<Pattern> patterns;
        }

        public ValuesAndPatternsSet load() {
            return new ValuesAndPatternsSet(load(getValues(), getValueFiles()).stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet()),
                    load(getPatterns(), getPatternFiles()).stream()
                            .distinct()
                            .map(Pattern::compile)
                            .collect(Collectors.toSet()));
        }

        static Set<String> load(Set<String> plain, Set<String> files) {
            Set<String> values = new HashSet<>(plain);
            for (String file : files) {
                try (BufferedReader bufferedReader = LocalFiles.newBufferedReader(Paths.get(file))) {
                    CSVParser records = CSVFormat.DEFAULT.parse(bufferedReader);
                    for (CSVRecord record : records) {
                        values.add(record.get(0));
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read ValuesAndPatternsSet from file " + file, e);
                }
            }
            return values;
        }
    }

    @Data
    public static class MatchingMaps {
        MatchingMap guard = new MatchingMap();
        MatchingSet ignore = new MatchingSet();
    }

    @Data
    public static class MatchingMap {
        Map<String, String> values = new HashMap<>();
        Map<String, String> patterns = new HashMap<>();
        Set<String> valueFiles = new HashSet<>();
        Set<String> patternFiles = new HashSet<>();

        @Value
        public static class ValuesAndPatternsMap {
            Map<String, String> values;
            Map<Pattern, String> patterns;
        }

        final static BinaryOperator<String> MERGE_TEMPLATES = (v1, v2) -> {
            if (StringUtils.isBlank(v1)) {
                return v2;
            }
            if (StringUtils.isBlank(v2)) {
                return v1;
            }
            if (!Objects.equals(v1, v2)) {
                log.error("Different templates configured for same key, using last one: {}, {}", v1, v2);
            }
            return v2;
        };

        public ValuesAndPatternsMap load() {

            return new ValuesAndPatternsMap(load(getValues(), getValueFiles()).entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey().toLowerCase(), Map.Entry::getValue, MERGE_TEMPLATES)),
                    load(getPatterns(), getPatternFiles()).entrySet().stream()
                            .collect(Collectors.toMap(e -> Pattern.compile(e.getKey()), Map.Entry::getValue, MERGE_TEMPLATES)));
        }

        static Map<String, String> load(Map<String, String> plain, Set<String> files) {
            Map<String, String> values = new HashMap<>(plain);
            for (String file : files) {
                try (BufferedReader bufferedReader = LocalFiles.newBufferedReader(Paths.get(file))) {
                    CSVParser records = CSVFormat.DEFAULT.withCommentMarker('#').parse(bufferedReader);
                    for (CSVRecord record : records) {
                        String key = record.get(0);
                        String value = record.size() > 1 ? record.get(1) : "";
                        values.merge(key, StringUtils.isBlank(value) ? "" : value.trim(), MERGE_TEMPLATES);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read ValuesAndPatternsMap from file " + file, e);
                }
            }
            return values;
        }
    }

    @Data
    public static class InstructionConfig {
        Set<File> files = new HashSet<>();
    }


    @Data
    public static class CsvFileConfig {
        String filename;
        Map<String, String> dictionary = new LinkedHashMap<>();
        Map<String, String> columns = new LinkedHashMap<>();

        public boolean matches(String filenameToMatch) {
            return FilePathMatcher.match(filename, filenameToMatch);
        }
    }

    @Data
    public static class CsvReplacementConfig {
        boolean enabled = false;
        char delim = ',';
        char quote = '"';
        Character commentMarker = null;
        File dictionaryCandidate;
        List<CsvFileConfig> files = new ArrayList<>();

        public CsvFileConfig get(String filename) {
            return files.stream().filter(f -> f.matches(filename)).findFirst().orElse(null);
        }
    }

    @Data
    public static class CopyChangesConfig {
        SourceConfig privateSource;
        SourceConfig publicSource;
        MatchingSets file = new Config.MatchingSets();
    }

    @Data
    public static class SourceConfig {
        String dir;
        CopyMode mode;
        String branchPrefix;
        String tag;
        BranchTagConfig egress;
        BranchTagConfig ingress;
    }

    @Data
    public static class BranchTagConfig {
        String date;
        String tag;
        String main;
        String branch;
        TaggedBranch staging;
        String startTag;
    }

    @Data
    public static class TaggedBranch {
        String name;
        String tag;
    }

    public enum CopyMode {
        GIT, FILES, FILTERED_FILES
    }
}
