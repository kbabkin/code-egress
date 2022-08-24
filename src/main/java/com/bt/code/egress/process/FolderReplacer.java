package com.bt.code.egress.process;

import com.bt.code.egress.read.FilePathMatcher;
import com.bt.code.egress.report.Stats;
import com.bt.code.egress.write.FileCompleted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Slf4j
public class FolderReplacer {
    private final FileReplacer fileReplacer;
    private final CsvFileReplacer csvFileReplacer;
    private final FilePathMatcher filePathMatcher;
    private final FileCompleted.Listener fileCompletedListener;

    public void replace(FileLocation folder) {
        replace(folder, folder);
    }

    void replace(FileLocation folder, FileLocation rootFolder) {
        if (!Files.isDirectory(folder.getFilePath())) {
            throw new RuntimeException("Not a folder: " + folder);
        }
        try (Stream<FileLocation> files = folder.list()) {
            files.forEach(file -> {
                FileLocation relativeFile = rootFolder.relativize(file);

                String name = relativeFile.toString().toLowerCase();
                if (Files.isDirectory(file.getFilePath())) {
                    if (filePathMatcher.match(name + "/")) {
                        replace(file, rootFolder);
                    } else {
                        log.info("Ignore folder: {}", relativeFile);
                        Stats.folderIgnored();
                    }
                    return;
                }
                if (!filePathMatcher.match(name)) {
                    log.info("Ignore file: {}", relativeFile);
                    Stats.fileIgnored();
                    return;
                }
                if (isZipFile(file.getFilePath())) {
                    processZip(file.getFilePath(), relativeFile.getFilePath(), false, false);
                } else {
                    //we need csv replacer then regular replacer (or visa versa -) )
                    //TODO create a chain of 2 replacers
//                    if (csvFileReplacer.isEnabled()) {
//                        try {
//                            if (csvFileReplacer.isEligibleForReplacement(file.getFilename())) {
//                                FileCompleted fileCompleted = csvFileReplacer.replace(file);
//                                fileCompletedListener.onFileCompleted(fileCompleted);
//                            }
//                        } catch (IOException e) {
//                            throw new RuntimeException("Failed to process file: " + relativeFile, e);
//                        }
//
//                    }
//                    else {
                        try (BufferedReader bufferedReader = Files.newBufferedReader(file.getFilePath())) {
                            FileCompleted fileCompleted = fileReplacer.replace(relativeFile, bufferedReader);
                            fileCompletedListener.onFileCompleted(fileCompleted);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to process file: " + relativeFile, e);
                        }
//                    }
                }
            });
        }
    }

    private void processZip(Path file, Path relativeFile, boolean createBackup, boolean unpack) {
        log.info("Processing ZIP file: {}", file);

        //Maybe we will need backups in 'override' tool mode, skipping in 'empty folder' mode
        if (createBackup) {
            try {
                Files.copy(file, Paths.get(file + ".bak"));
            } catch (IOException e) {
                log.warn("Failed to create backup copy of {}", file, e);
            }
        }

        try (FileLocation zipRoot = FileLocation.forZipRoot(file, relativeFile)) {
            //Read and scan zip contents as it were unpacked, but take care of further writes
            replace(zipRoot);
            //auto close zip in the end
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to process zip file: %s", file), e);
        }

        //Maybe we will need unpacked contents in 'override' tool mode, skipping in 'empty folder' mode
        if (unpack) {
            String targetDir = file + ".unzip";
            try {
                Files.createDirectories(Paths.get(targetDir));
                try (ZipFile zipFile = new ZipFile(file.toFile())) {
                    zipFile.extractAll(targetDir);
                }
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