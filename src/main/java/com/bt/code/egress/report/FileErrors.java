package com.bt.code.egress.report;

import com.bt.code.egress.file.LocalFiles;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

@UtilityClass
@Slf4j
public class FileErrors {
    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.withHeader("File", "Message");

    @Getter
    private final Map<String, Set<String>> messages = new ConcurrentHashMap<>();

    public void addError(String file, String message) {
        messages.compute(file, (f, messages) -> {
            if (messages == null) {
                messages = new ConcurrentSkipListSet<>();
            }
            messages.add(message);
            return messages;
        });
    }

    public void dump(Path path) {
        Stats.increment("File Errors", messages.values().stream()
                .map(Collection::size)
                .reduce(0, Integer::sum));
        TreeMap<String, Set<String>> sortedFiles = new TreeMap<>(messages);
        if (!messages.isEmpty()) {
            log.info("File messages:\n{}", sortedFiles.entrySet().stream()
                    .map(e -> e.getKey() + ":\n\t" + String.join("\n\t", e.getValue()))
                    .collect(Collectors.joining("\n")));
        }
        if (path == null) {
            return;
        }
        try {
            LocalFiles.createDirectories(path.getParent());
            try (BufferedWriter writer = LocalFiles.newBufferedWriter(path)) {
                try (CSVPrinter printer = new CSVPrinter(writer, CSV_FORMAT)) {
                    for (String file : sortedFiles.keySet()) {
                        List<String> messages = new ArrayList<>(sortedFiles.get(file));
                        messages.sort(Comparator.naturalOrder());
                        for (String message : messages) {
                            printer.printRecord(file, message);
                        }
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to write file errors " + path, e);
        }

    }

    public void reset() {
        messages.clear();
    }

}
