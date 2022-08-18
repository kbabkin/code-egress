package com.bt.code.egress.process;

import com.bt.code.egress.read.WordGuardIgnoreMatcher;
import com.bt.code.egress.write.FileCompleted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

@RequiredArgsConstructor
@Slf4j
public class FolderReplacer {
    private final FileReplacer fileReplacer;
    private final WordGuardIgnoreMatcher fileMatcher;
    private final FileCompleted.Listener fileCompletedListener;

    public void replace(Path folder) {
        replace(folder, folder);
    }

    void replace(Path folder, Path rootFolder) {
        if (!Files.isDirectory(folder)) {
            throw new RuntimeException("Not a folder: " + folder);
        }
        try (Stream<Path> files = Files.list(folder)) {
            files.forEach(file -> {
                String relativeFile = rootFolder.relativize(file).toString();

                String name = file.getFileName().toString().toLowerCase();
                String matchReason = fileMatcher.getMatchReason(name);
                if (matchReason == null) {
                    //todo log ignore
                    log.info("Ignore file: {}", relativeFile);
                    return;
                }
                if (Files.isDirectory(file)) {
                    replace(file, rootFolder);
                } else if (isZipFile(file)) {
                    log.info("Read ZIP file: {}", relativeFile);
                    try (ZipFile zipFile = new ZipFile(file.toFile())) {
                        zipFile.stream().forEach(zipEntry -> {
                            //todo process
                            log.info("TODO Read entry: {}!{}", relativeFile, zipEntry);
                        });
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to process ZIP file: " + relativeFile, e);
                    }
                } else {
                    try (BufferedReader bufferedReader = Files.newBufferedReader(file)) {
                        FileCompleted fileCompleted = fileReplacer.replace(relativeFile, bufferedReader);
                        fileCompletedListener.onFileCompleted(fileCompleted);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to process file: " + relativeFile, e);
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    static boolean isZipFile(Path file) {
        byte[] bytes = new byte[4];
        try (FileInputStream fIn = new FileInputStream(file.toFile())) {
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