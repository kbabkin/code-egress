package com.bt.code.egress.write;

import com.bt.code.egress.report.Stats;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class FolderWriter implements FileCompleted.Listener {
    @Getter
    private final Path root;

    @Override
    public void onFileCompleted(FileCompleted fileCompleted) {
        init();
        if (fileCompleted.isChanged()) {
            if (fileCompleted.getFile().isInsideZip()) {
                writeIntoZip(fileCompleted.getFile().getRelativeZipPath(),
                        fileCompleted.getFile().getFilePath(), fileCompleted.getReplacedLines());
            } else {
                write(fileCompleted.getFile().getFilePath(), fileCompleted.getReplacedLines());
            }
            Stats.fileChanged();
        }
        Stats.fileRead();
        Stats.bytesRead(
                fileCompleted.getOriginalLines().stream().mapToInt(String::length).sum()
        );

        if (fileCompleted.getFile().isCsv()) {
            Stats.csvFileRead();
        }
    }

    public void write(Path file, List<String> replacedLines) {
        init();
        Path path = root.resolve(file);
        log.info("Save changed file to {}", path);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, replacedLines);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write output to " + path, e);
        }
    }

    public void writeIntoZip(Path originalZipPath, Path file, List<String> replacedLines) {
        Path newZipPath = root.resolve(originalZipPath);
        log.info("For originalZipPath {}, will write {} to target {}", originalZipPath, file, newZipPath);

        ZipFile zipFile = new ZipFile(newZipPath.toFile());
        ZipParameters parameters = new ZipParameters();
        parameters.setFileNameInZip(file.toString());
        if (!Files.exists(newZipPath)) {
            createEmptyZip(newZipPath);
        }

        try {
            zipFile.addStream(toStream(replacedLines), parameters);
            zipFile.close();
        } catch (IOException ie) {
            throw new RuntimeException(String.format("Failed to add file %s to zip %s", file, newZipPath), ie);
        }

        log.info("Save changed file {} to {}", file, newZipPath);
    }

    private InputStream toStream(List<String> lines) {
        return new ByteArrayInputStream(String.join(System.lineSeparator(), lines).getBytes(StandardCharsets.UTF_8));
    }

    final static byte[] EMPTY_ZIP_BYTES = {80, 75, 05, 06, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00};

    public static void createEmptyZip(Path path) {
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not create directories for empty zip %s", path));
        }

        try {
            OutputStream fos = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW);
            fos.write(EMPTY_ZIP_BYTES, 0, 22);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not create empty zip %s", path));
        }
    }

    void init() {
        // do nothing by default
    }

    public void verify() {
        // do nothing by default
    }
}
