package com.bt.code.egress.process;

import com.bt.code.egress.read.WordMatch;
import com.bt.code.egress.report.Stats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Slf4j
public class WordReplacer {
    private final Map<String, String> generatedMap = new ConcurrentHashMap<>();
    private final String defaultTemplate;

    public void saveGenerated(Path generatedReplacementsPath) {
        if (generatedMap.isEmpty()) {
            log.info("No generated replacements");
            return;
        }

        log.info("Writing generated replacements to {}", generatedReplacementsPath);
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
        Stats.increment("Generated Replacements", generatedMap.size());
    }

    public String replace(WordMatch wordMatch) {
        if (wordMatch.getReplacement() != null) {
            return wordMatch.getReplacement(); // for csv matches
        }

        String word = wordMatch.getLineToken().getWordLowerCase();
        String generated = generatedMap.get(word);
        if (generated != null) {
            return generated;
        }
        String predefined = wordMatch.getTemplate();
        String template = StringUtils.isBlank(predefined) ? defaultTemplate : predefined;

        return generate(word, template);
    }

    private String generate(String word, String template) {
        int hashCode;
        if (word.length() <= 0) {
            hashCode = 0;
        } else {
            String hashWord = word;
            while (hashWord.length() < 20) {
                hashWord += hashWord;
            }
            hashCode = hashWord.hashCode();
        }
        StringSubstitutor substitutor = new StringSubstitutor(
                Collections.singletonMap("hash", Math.abs(hashCode)),
                "{", "}");
        String generated = substitutor.replace(template);
        if (!generated.equals(template)) {
            generatedMap.put(word, generated);
        }
        return generated;
    }

}
