package com.bt.code.egress.process;

import com.bt.code.egress.Config;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class WordReplacer {
    private final Map<String, String> predefinedMap;
    private final Map<String, String> generatedMap = new HashMap<>();

    int num = 1; //todo init

    public static WordReplacer fromConfig(Config.MapGroup mapGroup) {
        return new WordReplacer(load(mapGroup.getValues(), mapGroup.getValueFiles()));
    }

    static Map<String, String> load(Map<String, String> plain, Set<String> files) {
        Map<String, String> values = new HashMap<>(plain);
        for (String file : files) {
            try (InputStream inputStream = Files.newInputStream(Paths.get(file))) {
                CSVParser records = CSVFormat.DEFAULT.parse(new InputStreamReader(inputStream));
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
        String value = "w" + num++;
        generatedMap.put(word, value);
        return value;
    }

}
