package com.bt.code.egress.report;

import lombok.Getter;
import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@UtilityClass
public class Stats {
    @Getter
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

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
}
