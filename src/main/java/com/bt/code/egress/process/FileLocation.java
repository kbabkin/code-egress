package com.bt.code.egress.process;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Data
@AllArgsConstructor
@Builder
@Slf4j
public class FileLocation implements AutoCloseable {
    private Path filePath;

    private FileSystem zipFileSystem;
    private Path zipPath;
    private Path relativeZipPath;
    private Path rootInsideZip;

    private FileLocation originalLocation;

    @Override
    public String toString() {
        return toReportedPath();
    }

    public String toReportedPath() {
        if (!isInsideZip()) {
            return filePath.toString();
        } else {
            return relativeZipPath.toString() + ":" + filePath;
        }
    }

    public boolean isCsv() {
        return getFilePath().toString().toLowerCase().endsWith(".csv");
    }

    public Stream<FileLocation> list() {
        try {
            Stream<Path> files = Files.list(filePath);
            if (isInsideZip()) {
                return files.map(p -> FileLocation
                        .builder()
                        .filePath(p)
                        .zipFileSystem(zipFileSystem)
                        .zipPath(zipPath)
                        .relativeZipPath(relativeZipPath)
                        .rootInsideZip(rootInsideZip)
                        .build());

            } else {
                return files.map(FileLocation::forFile);
            }
        } catch (IOException ie) {
            throw new RuntimeException(String.format("Failed to list files for %s", filePath), ie);
        }
    }

    public FileLocation relativize(FileLocation other) {
        if (!other.isInsideZip() && !isInsideZip()) {
            FileLocation result = FileLocation.forFile(getFilePath().relativize(other.getFilePath()));
            result.setOriginalLocation(other);
            return result;
        } else if (other.isInsideZip() && isInsideZip()) {
            FileLocation result = FileLocation.withOtherPathInsideZip(this, getFilePath().relativize(other.getFilePath()));
            result.setOriginalLocation(other);
            return result;
        } else {
            throw new IllegalArgumentException ("Invalid usage of FileLocation API : relativize() arguments must be both zip-based or not zip-based");
        }
    }

    public boolean isInsideZip() {
        return zipPath != null;
    }

    @Override
    public void close() throws Exception {
        if (zipFileSystem != null) {
            try {
                zipFileSystem.close();
            } catch (IOException ie) {
                log.error("Failed to close zip file system for {}", zipPath, ie);
            }
        }
    }

    public String getFilename() {
        return filePath.getFileName().toString();
    }

    public static FileLocation forFile(File file) {
        return forFile(file.toPath());
    }

    public static FileLocation forFile(Path filePath) {
        return FileLocation.builder().filePath(filePath).build();
    }

    public static FileLocation forZipRoot(Path zipFile, Path relativeZipFile) throws IOException {
        FileSystem zipFileSystem = FileSystems.newFileSystem(zipFile, (ClassLoader) null);

        Path rootInsideZip = zipFileSystem.getPath("/");
        return FileLocation
                .builder()
                .zipPath(zipFile)
                .relativeZipPath(relativeZipFile)
                .zipFileSystem(zipFileSystem)
                .rootInsideZip(rootInsideZip)
                .filePath(rootInsideZip)
                .build();
    }
    public static FileLocation withOtherPathInsideZip(FileLocation location, Path otherPathInsideZip) {

        if (!location.isInsideZip()) {
            throw new IllegalArgumentException("Invalid usage of FileLocation API - location should be zip-based: " + location);
        }

        return FileLocation
                .builder()
                .zipPath(location.getZipPath())
                .relativeZipPath(location.getRelativeZipPath())
                .zipFileSystem(location.getZipFileSystem())
                .rootInsideZip(location.getRootInsideZip())
                .filePath(otherPathInsideZip)
                .build();
    }

}
