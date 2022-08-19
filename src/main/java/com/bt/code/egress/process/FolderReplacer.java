package com.bt.code.egress.process;

import com.bt.code.egress.read.WordGuardIgnoreMatcher;
import com.bt.code.egress.write.FileCompleted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.stream.Stream;
import net.lingala.zip4j.ZipFile;

@RequiredArgsConstructor
@Slf4j
public class FolderReplacer {
    private final FileReplacer fileReplacer;
    private final WordGuardIgnoreMatcher fileMatcher;
    private final CsvFileReplacer csvFileReplacer;
    private final GroupMatcher fileMatcher;
    private final FileCompleted.Listener fileCompletedListener;

    private final ZipRegistry zipRegistry;

    public void replace(Path folder) {
        replace(folder, folder);
    }

    void replace(Path folder, Path rootFolder) {
        if (!Files.isDirectory(folder)) {
            throw new RuntimeException("Not a folder: " + folder);
        }
        try (Stream<Path> files = Files.list(folder)) {
            files.forEach(file -> {
                Path relativeFile = rootFolder.relativize(file);

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
                    processZip(file, relativeFile, false, false);
                } else {
                    //we need csv replacer then regular replacer (or visa versa -) )
                    //TODO create a chain of 2 replacers
                    if (csvFileReplacer.isEnabled()) {
                        try {
                            if (csvFileReplacer.isEligibleForReplacement(file.getFileName().toString())) {
                                FileCompleted fileCompleted = csvFileReplacer.replace(file);
                                fileCompletedListener.onFileCompleted(fileCompleted);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to process file: " + relativeFile, e);
                        }

                    } else {
                        try (BufferedReader bufferedReader = Files.newBufferedReader(file)) {
                            FileCompleted fileCompleted = fileReplacer.replace(relativeFile, bufferedReader);
                            fileCompletedListener.onFileCompleted(fileCompleted);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to process file: " + relativeFile, e);
                        }
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to process folder: " + folder, e);
        }
    }

    private void processZip(Path file, Path relativeFile, boolean createBackup, boolean unpack)  {
        log.info("Processing ZIP file: {}", file);

        //Maybe we will need backups in 'override' tool mode, skipping in 'empty folder' mode
        if (createBackup) {
            try {
                Files.copy(file, Paths.get(file + ".bak"));
            } catch (IOException e) {
                log.warn("Failed to create backup copy of {}", file, e);
            }
        }

        try  {
            Path zipRoot = zipRegistry.register(file, relativeFile);
            //Read and scan zip contents as it were unpacked, but take care of further writes
            replace(zipRoot);
        } catch (IOException ie) {
            throw new RuntimeException(String.format("Failed to process zip file: %s", file), ie);
        }

        //Maybe we will need unpacked contents in 'override' tool mode, skipping in 'empty folder' mode
        if (unpack) {
            String targetDir = file + ".unzip";
            try {
                Files.createDirectories(Paths.get(targetDir));
                (new ZipFile(file.toFile()))
                        .extractAll(targetDir);
            } catch (IOException e) {
                log.error("Could not create folder {} with unpacked content for {}", targetDir, file, e);
            }
        }

        log.info("ZIP file: {} processed", file);
    }

    static boolean isZipFile(Path file) {
        byte[] bytes = new byte[4];
        try (InputStream fIn = Files.newInputStream(file)) {
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