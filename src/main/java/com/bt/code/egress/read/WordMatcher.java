package com.bt.code.egress.read;

import com.bt.code.egress.Config;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Value
@NonFinal
public class WordMatcher {
    Set<String> values;
    List<Pattern> patterns;

    public static WordMatcher fromConfig(Config.MatchingGroup matchingGroup) {
        return new WordMatcher(load(matchingGroup.getValues(), matchingGroup.getValueFiles()).stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet()),
                load(matchingGroup.getPatterns(), matchingGroup.getPatternFiles()).stream()
                        .map(Pattern::compile)
                        .collect(Collectors.toList()));
    }

    static Set<String> load(Set<String> plain, Set<String> files) {
        Set<String> values = new HashSet<>(plain);
        for (String file : files) {
            try (InputStream inputStream = Files.newInputStream(Paths.get(file))) {
                CSVParser records = CSVFormat.DEFAULT.parse(new InputStreamReader(inputStream));
                for (CSVRecord record : records) {
                    values.add(record.get(0));
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read config from file " + file, e);
            }
        }
        return values;
    }

    public String getMatchReason(String word) {
        if (values.contains(word)) {
            return "Exact match: " + word;
        }
        for (Pattern pattern : patterns) {
            if (pattern.matcher(word).matches()) {
                return "Pattern match: " + pattern;
            }
        }
        return null;
    }
}
