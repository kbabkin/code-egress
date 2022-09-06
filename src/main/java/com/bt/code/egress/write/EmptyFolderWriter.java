package com.bt.code.egress.write;

import com.bt.code.egress.report.Stats;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Slf4j
public class EmptyFolderWriter extends FolderWriter {
    private boolean inited;
    private boolean wasEmpty;

    public EmptyFolderWriter(Path root) {
        super(root);
    }

    @Override
    void init() {
        if (inited) {
            return;
        }
        try {
            if (Files.exists(getRoot())) {
                try (Stream<Path> list = Files.list(getRoot())) {
                    long count = list.count();
                    wasEmpty = count == 0;

                }
            } else {
                Files.createDirectories(getRoot());
                wasEmpty = true;
            }
            inited = true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to use output folder " + getRoot(), e);
        }

    }

    @Override
    public void verify() {
        init();
        if (!wasEmpty) {
            log.warn("!!!WARNING!!! write folder {} was not empty, it can contain files from previous scans!", getRoot());
        }
    }

    @Override
    protected void prepareZip(Path sourceZipPath, Path newZipPath) {
        try {
            if (!Files.exists(newZipPath.getParent())) {
                Files.createDirectories(newZipPath.getParent());
            }
            if (!Files.exists(newZipPath)) {
                Files.copy(sourceZipPath, newZipPath);
            }
        } catch (IOException e) {
            log.error("Could not copy {} to {}", sourceZipPath, newZipPath, e);
            Stats.addError(newZipPath.toString(), String.format("Could not copy %s to %s", sourceZipPath, newZipPath));
        }
    }
}
