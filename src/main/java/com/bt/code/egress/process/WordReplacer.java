package com.bt.code.egress.process;

import com.bt.code.egress.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@RequiredArgsConstructor
@Slf4j
public class WordReplacer {
    private final Map<String, String> predefinedMap;
    private final Map<String, String> generatedMap = new HashMap<>();

//    int num = 1; //todo init

    public static WordReplacer fromConfig(Config.MapGroup mapGroup) {
        return new WordReplacer(load(mapGroup.getValues(), mapGroup.getValueFiles()));
    }

    static Map<String, String> load(Map<String, String> plain, Set<String> files) {
        Map<String, String> values = new HashMap<>(plain);
        for (String file : files) {
            try (BufferedReader bufferedReader = Files.newBufferedReader(Paths.get(file))) {
                CSVParser records = CSVFormat.DEFAULT.parse(bufferedReader);
                for (CSVRecord record : records) {
                    String value = record.get(1);
                    if (value != null && value.trim().length() > 0) {
                        values.put(record.get(0), value);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read config from file " + file, e);
            }
        }
        return values;
    }

    public void saveGenerated(Path generatedReplacementsPath) {
        if (generatedMap.isEmpty()) {
            log.info("No generated replacements");
            return;
        }

        log.info("Writing {} generated replacements to {}", generatedMap.size(), generatedReplacementsPath);
        TreeMap<String, String> sorted = new TreeMap<>(generatedMap);
        try (BufferedWriter writer = Files.newBufferedWriter(generatedReplacementsPath)) {
            try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
                for (Map.Entry<String, String> entry : sorted.entrySet()) {
                    printer.printRecord(entry.getKey(), entry.getValue());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write generated replacements to " + generatedReplacementsPath, e);
        }
    }

    public String replace(String word) {
//            return Optional.ofNullable(suggested)
//                    .orElseGet(() -> predefinedMap.get(word))
//                    .orElseGet(() -> generatedMap.get(word))
//                    .orElseGet(this::generate);
        String predefined = predefinedMap.get(word);
        if (predefined != null) {
            return predefined;
        }
        String generated = generatedMap.get(word);
        if (generated != null) {
            return generated;
        }
        return generate(word);
    }

    // todo hashcode?
    // todo: by pattern
    private String generate(String word) {
//        String value = "w" + num++;
        String value = "w" + Math.abs(word.hashCode());
        generatedMap.put(word, value);
        return value;
    }

}
