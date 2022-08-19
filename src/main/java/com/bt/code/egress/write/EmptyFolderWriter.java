package com.bt.code.egress.write;

import com.bt.code.egress.report.Stats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Slf4j
public class EmptyFolderWriter implements FileCompleted.Listener {
    private final Path root;
    private boolean inited;
    private boolean wasEmpty;

    @Override
    public void onFileCompleted(FileCompleted fileCompleted) {
        init();
        if (fileCompleted.isChanged()) {
            write(fileCompleted.getFile(), fileCompleted.getReplacedLines());
            Stats.fileChanged();
        }
        Stats.fileRead();
    }

    private void write(String file, List<String> replacedLines) {
        Path path = root.resolve(file);
        log.info("Save changed file to {}", path);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, replacedLines);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write output to " + path, e);
        }
    }

    private void init() {
        if (inited) {
            return;
        }
        try {
            if (Files.exists(root)) {
                try (Stream<Path> list = Files.list(root)) {
                    long count = list.count();
                    wasEmpty = count == 0;

                }
            } else {
                Files.createDirectories(root);
                wasEmpty = true;
            }
            inited = true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to use output folder " + root, e);
        }

    }

    public void verify() {
        init();
        if (!wasEmpty) {
            log.warn("!!!WARNING!!! write folder {} was not empty, it can contain files from previous scans!", root);
        }
    }
}
