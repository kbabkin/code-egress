package com.bt.code.egress.report;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@UtilityClass
@Slf4j
public class Stats {
    @Getter
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, Set<String>> errors = new ConcurrentHashMap<>();

    public void increment(String name, int byValue) {
        AtomicLong value = counters.computeIfAbsent(name, k -> new AtomicLong());
        value.addAndGet(byValue);
    }

    public void increment(String name) {
        increment(name, 1);
    }

    public void fileRead() {
        increment("Files Read");
    }

    public void fileChanged() {
        increment("Files Changed");
    }

    public void zipFileRead() {
        increment("Zip Files Read");
    }

    public void csvFileRead() {
        increment("Csv Files Read");
    }

    public void csvFileWithColumnReplacements() {
        increment("Csv Files With Configured Column Replacements Detected");
    }

    public void bytesRead(int bytes) {
        increment("Bytes Read", bytes);
    }

    public void linesRead(int lines) {
        increment("Lines Read", lines);
    }

    public void fileFailed() {
        increment("Failed Files");
    }

    public void fileIgnored() {
        increment("Ignored Files");
    }

    public void folderIgnored() {
        increment("Ignored Folders");
    }

    public void wordReplaced() {
        increment("Words Replaced");
    }

    public void wordsMatched(int byValue) {
        increment("Words Matched", byValue);
    }

    public void wordsFalsePositive(int byValue) {
        increment("Words False Positive", byValue);
    }

    public void wordConflict() {
        increment("Words Conflicts");
    }

    public void addError(String file, String message) {

    }

    public void dump() {
        if (!errors.isEmpty()) {
            log.info("File errors: ");
            StringBuilder sbErrors = new StringBuilder();
            for (String file : errors.keySet()) {
                sbErrors.append("\n=======================================================\n");
                sbErrors.append(String.format("%d error(s) in %s\n",
                        errors.get(file).size(),
                        file));
                sbErrors.append("=======================================================\n\t");
                sbErrors.append(String.join("\n\t", errors.get(file)));
            }
            log.info(sbErrors.toString());
        }

        log.info("Counters: \n\t{}", new TreeMap<>(Stats.getCounters()).entrySet().stream()
                .map(String::valueOf).collect(Collectors.joining("\n\t")));
    }

}
