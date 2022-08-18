package com.bt.code.egress.process;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class ZipRegistry {
    private List<ZipRegistryItem> items = Lists.newArrayList();

    public Path register(Path zipPath, Path relativeZipPath) throws IOException {
        FileSystem zipFileSystem = FileSystems.newFileSystem(zipPath, null);
        Path rootInsideZip = zipFileSystem.getPath("/");
        items.add(new ZipRegistryItem(zipFileSystem, zipPath, relativeZipPath, rootInsideZip));
        return rootInsideZip;
    }

    @Nullable
    public ZipRegistryItem findRelatedZip(Path pathInsideZip) {
        return items
                .stream()
                .filter(item -> item.getZipFileSystem() == pathInsideZip.getFileSystem())
                .findFirst()
                .orElse(null);
    }

    public void close() {
        items.forEach(item -> {
            try {
                if (item.getZipFileSystem() != null) {
                    item.getZipFileSystem().close();
                }
            } catch (IOException ie) {
                log.warn("Could not close file system for {}", item.getZipPath(), ie);
            }
        });
    }

    @AllArgsConstructor
    @Data
    public static class ZipRegistryItem {
        private FileSystem zipFileSystem;
        private Path zipPath;
        private Path relativeZipPath;
        private Path rootInsideZip;
    }
}

