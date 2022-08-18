package com.bt.code.egress;

import lombok.Data;
import lombok.Value;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Configuration
@EnableConfigurationProperties
@ConfigurationProperties
@Data
public class Config {
    MatchingGroups read = new MatchingGroups();
    MatchingGroups word = new MatchingGroups();
    MapGroup replace = new MapGroup();
    Allow allow = new Allow();

    @Data
    public static class MatchingGroups {
        MatchingGroup guard = new MatchingGroup();
        MatchingGroup ignore = new MatchingGroup();
    }

    @Data
    public static class MatchingGroup {
        Set<String> values = new HashSet<>();
        Set<String> patterns = new HashSet<>();
        Set<String> valueFiles = new HashSet<>();
        Set<String> patternFiles = new HashSet<>();
    }

    @Data
    public static class MapGroup {
        Map<String, String> values = new HashMap<>();
        Set<String> valueFiles = new HashSet<>();
    }

    @Data
    public static class Allow {
        Set<String> reportFiles = new HashSet<>();
    }

    @Value
    public static class ValuesAndPatterns {
        Set<String> values;
        List<Pattern> patterns;

        public static ValuesAndPatterns fromConfig(Config.MatchingGroup matchingGroup) {
            return new ValuesAndPatterns(load(matchingGroup.getValues(), matchingGroup.getValueFiles()).stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet()),
                    load(matchingGroup.getPatterns(), matchingGroup.getPatternFiles()).stream()
                            .distinct()
                            .map(Pattern::compile)
                            .collect(Collectors.toList()));
        }

        static Set<String> load(Set<String> plain, Set<String> files) {
            Set<String> values = new HashSet<>(plain);
            for (String file : files) {
                try (BufferedReader bufferedReader = Files.newBufferedReader(Paths.get(file))) {
                    CSVParser records = CSVFormat.DEFAULT.parse(bufferedReader);
                    for (CSVRecord record : records) {
                        values.add(record.get(0));
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read config from file " + file, e);
                }
            }
            return values;
        }

    }

}
