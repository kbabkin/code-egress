package com.bt.code.egress.process;

import com.bt.code.egress.read.FilePathMatcher;
import com.bt.code.egress.read.LineLocation;
import com.bt.code.egress.read.LineToken;
import com.bt.code.egress.report.Stats;
import com.bt.code.egress.write.FileCompleted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Slf4j
public class FolderReplacer {
    private final CsvFileReplacer fileReplacer;
    private final FilePathMatcher filePathMatcher;
    private final FilePathMatcher allowFilePathMatcher;
    private final TextMatched.Listener textMatchedListener;
    private final FileCompleted.Listener fileCompletedListener;

    public void replace(FileLocation folder, BiConsumer<String, Runnable> submitter) {
        replace(folder, folder, submitter);
    }

    void replace(FileLocation folder, FileLocation rootFolder, BiConsumer<String, Runnable> submitter) {
        if (!Files.isDirectory(folder.getFilePath())) {
            throw new RuntimeException("Not a folder: " + folder);
        }
        try (Stream<FileLocation> files = folder.list()) {
            files.forEach(file -> {
                FileLocation relativeFile = rootFolder.relativize(file);

                String name = relativeFile.toString().toLowerCase();
                if (Files.isDirectory(file.getFilePath())) {
                    if (filePathMatcher.match(name + "/")) {
                        replace(file, rootFolder, submitter);
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
                if (allowFilePathMatcher.match(name)) {
                    log.info("Ignore file due to previous failure: {}", relativeFile);
                    Stats.fileFailed();
                    textMatchedListener.onMatched(new TextMatched(new LineLocation(relativeFile.toReportedPath(), 0),
                            new LineToken(""), true, "", "Ignore file due to previous failure"));
                    return;
                }
                if (isZipFile(file.getFilePath())) {
                    submitter.accept(relativeFile.toString(), () ->
                            processZip(file.getFilePath(), relativeFile.getFilePath()));
                } else {
                    submitter.accept(relativeFile.toString(), () -> {
                        try {
                            FileCompleted fileCompleted;
                            try (BufferedReader bufferedReader = Files.newBufferedReader(file.getFilePath())) {
                                fileCompleted = fileReplacer.replace(relativeFile, bufferedReader);
                            } catch (MalformedInputException e) {
                                log.error("UTF-8 encoding incompatible, trying ISO_8859_1: {}", relativeFile);
                                try (BufferedReader bufferedReader = Files.newBufferedReader(file.getFilePath(), StandardCharsets.ISO_8859_1)) {
                                    fileCompleted = fileReplacer.replace(relativeFile, bufferedReader);
                                }
                            }
                            fileCompletedListener.onFileCompleted(fileCompleted);
                        } catch (Exception e) {
                            log.error("Failed to process file {}", relativeFile, e);
                            Stats.fileFailed();
                            textMatchedListener.onMatched(new TextMatched(new LineLocation(relativeFile.toReportedPath(), 0),
                                    new LineToken(""), null, "", "FAILED to process file " + relativeFile));
                        }
                    });
                }
            });
        }
    }

    private void processZip(Path file, Path relativeFile) {
        log.info("Processing ZIP file: {}", file);

        try (FileLocation zipRoot = FileLocation.forZipRoot(file, relativeFile)) {
            //Read and scan zip contents as it were unpacked, but take care of further writes
            replace(zipRoot, JobRunner.DIRECT_RUNNER);
            //auto close zip in the end
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to process zip file: %s", file), e);
        }

        Stats.zipFileRead();
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