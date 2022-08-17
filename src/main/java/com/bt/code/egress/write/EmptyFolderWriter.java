package com.bt.code.egress.write;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class EmptyFolderWriter implements FileCompleted.Listener {
    private final Path root;

    @Override
    public void onFileCompleted(FileCompleted fileCompleted) {
        if (fileCompleted.isChanged()) {
            write(fileCompleted.getFile(), fileCompleted.getReplacedLines());
        }
    }

    public void write(String file, List<String> replacedLines) {
        Path path = root.resolve(file);
        log.info("Save changed file to {}", path);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, replacedLines);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write output to " + path, e);
        }
    }
}
