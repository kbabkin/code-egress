package com.bt.code.egress.write;

import com.bt.code.egress.file.KeepEolFiles;
import com.bt.code.egress.file.LocalFiles;
import com.bt.code.egress.report.FileErrors;
import com.bt.code.egress.report.Stats;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@RequiredArgsConstructor
@Slf4j
public class FolderWriter implements FileCompleted.Listener, ZipCompleted.Listener {
    @Getter
    private final Path root;
    private final Set<Path> preparedZips = new ConcurrentSkipListSet<>();

    @Override
    public void onFileCompleted(FileCompleted fileCompleted) {
        init();
        if (fileCompleted.isChanged()) {
            if (fileCompleted.getFile().isInsideZip()) {
                Path originalZipRelativePath = fileCompleted.getFile().getRelativeZipPath();
                Path newZipPath = getTargetZipRoot().resolve(originalZipRelativePath);

                prepareZip(fileCompleted.getFile().getZipPath(), newZipPath);
                writeIntoZip(originalZipRelativePath, newZipPath,
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
        Stats.linesRead(fileCompleted.getOriginalLines().size());

        if (fileCompleted.getFile().isCsv()) {
            Stats.csvFileRead();
        }
    }

    @Override
    public void onZipCompleted(ZipCompleted zipCompleted) {
        //Copy the resulting zip back in-place from temp dir
        Path newZipAbsolutePath = getTempRoot().resolve(zipCompleted.getSourceZipRelativePath());

        if (!LocalFiles.exists(newZipAbsolutePath)) {
            //No changes to this zip file - skipping
            return;
        }

        try {
            LocalFiles.move(
                    newZipAbsolutePath,
                    zipCompleted.getSourceZipAbsolutePath(),
                    StandardCopyOption.REPLACE_EXISTING);
            log.info("Moved zip {} to {}", newZipAbsolutePath, zipCompleted.getSourceZipAbsolutePath());

        } catch (IOException ie) {
            FileErrors.addError(zipCompleted.getSourceZipAbsolutePath().toString(),
                    String.format("Cannot replace %s due to %s",
                            zipCompleted.getSourceZipAbsolutePath(),
                            ie));
            log.error("Could not move {} to {}",
                    newZipAbsolutePath,
                    zipCompleted.getSourceZipAbsolutePath(), ie);
        }
    }

    public void write(Path file, List<String> replacedLines) {
        init();
        Path path = root.resolve(file);
        log.info("Save changed file to {}", path);
        try {
            LocalFiles.createDirectories(path.getParent());
            try (BufferedWriter bufferedWriter = LocalFiles.newBufferedWriter(path)) {
                KeepEolFiles.write(bufferedWriter, replacedLines);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write output to " + path, e);
        }
    }

    public void writeIntoZip(Path originalZipPath, Path newZipPath, Path file, List<String> replacedLines) {
        log.info("For originalZipPath {}, will write {} to target {}", originalZipPath, file, newZipPath);

        try (ZipFile zipFile = new ZipFile(newZipPath.toFile())) {
            ZipParameters parameters = new ZipParameters();
            parameters.setFileNameInZip(file.toString());

            zipFile.addStream(toStream(replacedLines), parameters);
        } catch (IOException ie) {
            throw new RuntimeException(String.format("Failed to add file %s to zip %s", file, newZipPath), ie);
        }

        log.info("Save changed file {} to {}", file, newZipPath);
    }

    protected void prepareZip(Path sourceZipPath, Path newZipPath) {
        if (!preparedZips.contains(sourceZipPath)) {
            try {
                if (!LocalFiles.exists(newZipPath.getParent())) {
                    LocalFiles.createDirectories(newZipPath.getParent());
                }
                //Overwrite any copies that might be left after previous runs
                LocalFiles.copy(sourceZipPath, newZipPath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Copied zip {} to {}", sourceZipPath, newZipPath);

                //Mark as prepared. Do not be scared of race conditions,
                // as files processing for a given zip is single-threaded
                preparedZips.add(sourceZipPath);
                Stats.zipFileChanged();
            } catch (IOException e) {
                log.error("Could not copy {} to {}", sourceZipPath, newZipPath, e);
                FileErrors.addError(newZipPath.toString(), String.format("Could not copy %s to %s", sourceZipPath, newZipPath));
            }
        }
    }


    private InputStream toStream(List<String> lines) {
        return new ByteArrayInputStream(String.join(System.lineSeparator(), lines).getBytes(StandardCharsets.UTF_8));
    }

    protected Path getTargetZipRoot() {
        return getTempRoot();
    }

    private Path getTempRoot() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        return Paths.get(tmpDir, "egress-tmp");
    }

    void init() {
        // do nothing by default
    }

    public void verify() {
        // do nothing by default
    }
}
