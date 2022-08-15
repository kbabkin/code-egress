package com.bt.code.egress.process;

import com.bt.code.egress.read.GroupMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.zip.ZipFile;

@RequiredArgsConstructor
@Slf4j
public class FolderReplacer {
    private final FileReplacer fileReplacer;
    private final GroupMatcher fileMatcher;

    public void replace(File folder) {
        if (!folder.isDirectory()) {
            throw new RuntimeException("Not a folder: " + folder);
        }
        File[] files = folder.listFiles();
        if (files == null) {
            throw new RuntimeException("No files at: " + folder);
        }
        for (File file : files) {
            // todo: relativize
            String name = file.getName();
            String matchReason = fileMatcher.getMatchReason(name);
            if (matchReason == null) {
                //todo log ignore
                log.info("Ignore file: {}", file);
                continue;
            }
            if (file.isDirectory()) {
                replace(file);
            } else if (isZipFile(file)) {
                log.info("Read ZIP file: {}", file);
                try (ZipFile zipFile = new ZipFile(file)) {
                    zipFile.stream().forEach(zipEntry -> {
                        //todo process
                        log.info("Read entry: {}", zipEntry);
                    });
                } catch (IOException e) {
                    throw new RuntimeException("Failed to process ZIP file: " + file, e);
                }
            } else {
                try (InputStream inputStream = Files.newInputStream(file.toPath())) {
                    fileReplacer.replace(file.getAbsolutePath(), inputStream);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to process file: " + file, e);
                }
            }
        }
    }

    static boolean isZipFile(File file) {
        byte[] bytes = new byte[4];
        try (FileInputStream fIn = new FileInputStream(file)) {
            if (fIn.read(bytes) != 4) {
                return false;
            }
            final int header = bytes[0] + (bytes[1] << 8) + (bytes[2] << 16) + (bytes[3] << 24);
            return 0x04034b50 == header;
        } catch (IOException e) {
            log.debug("Failed to check isZipFile: {}", file, e);
        }
        return false;
    }
}