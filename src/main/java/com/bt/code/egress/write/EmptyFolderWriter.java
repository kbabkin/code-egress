package com.bt.code.egress.write;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RequiredArgsConstructor
public class EmptyFolderWriter implements ReplacementWriter {
    private final Path root;

    @Override
    public void write(String file, List<String> replacedLines) {
        Path path = root.resolve(file);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, replacedLines);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write output to " + path, e);
        }
    }
}
