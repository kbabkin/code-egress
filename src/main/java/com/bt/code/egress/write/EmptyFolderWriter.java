package com.bt.code.egress.write;

import com.bt.code.egress.report.Stats;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
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
    protected Path getTargetZipRoot() {
        return getRoot();
    }

    @Override
    public void onZipCompleted(ZipCompleted zipCompleted) {
        //No additional actions required after all files updated in new zip
        // when we use empty folder
    }
}
