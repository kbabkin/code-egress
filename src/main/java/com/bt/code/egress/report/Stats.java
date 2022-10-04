package com.bt.code.egress.report;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@UtilityClass
@Slf4j
public class Stats {
    @Getter
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, Set<String>> messages = new ConcurrentHashMap<>();

    public void increment(String name, int byValue) {
        AtomicLong value = counters.computeIfAbsent(name, k -> new AtomicLong());
        value.addAndGet(byValue);
    }

    public void increment(String name) {
        increment(name, 1);
    }

    public void fileRead() {
        increment("Read Files");
    }

    public void fileChanged() {
        increment("Changed Files");
    }

    public void zipFileRead() {
        increment("Read Zip Files");
    }

    public void zipFileChanged() {
        increment("Changed Zip Files");
    }

    public void csvFileRead() {
        increment("Read Csv Files");
    }

    public void csvFileWithColumnReplacements() {
        increment("Changed Csv Column Template Files");
    }

    public void bytesRead(int bytes) {
        increment("Read Bytes", bytes);
    }

    public void linesRead(int lines) {
        increment("Read Lines", lines);
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

    public void wordConflict() {
        increment("Words Conflicts");
    }

    public void addError(String file, String message) {
        messages.compute(file, (f, messages) -> {
            if (messages == null) {
                messages = new ConcurrentSkipListSet<>();
            }
            messages.add(message);
            return messages;
        });
    }

    public void dump() {
        if (!messages.isEmpty()) {
            log.info("File messages:\n{}", new TreeMap<>(messages).entrySet().stream()
                    .map(e -> e.getKey() + ":\n\t" + String.join("\n\t", e.getValue()))
                    .collect(Collectors.joining("\n")));
        }

        log.info("Counters: \n\t{}", new TreeMap<>(Stats.getCounters()).entrySet().stream()
                .map(String::valueOf).collect(Collectors.joining("\n\t")));
    }

    public void reset() {
        counters.clear();
        messages.clear();
    }
}
