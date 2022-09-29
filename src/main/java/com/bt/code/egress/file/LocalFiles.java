package com.bt.code.egress.file;

import lombok.Getter;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.stream.Stream;

/**
 * File operations interface for testability.
 */
public class LocalFiles {
    @Getter
    @Setter
    private static LocalFilesImpl instance = new LocalFilesImpl();

    public static class LocalFilesImpl {
        public BufferedWriter newBufferedWriter(Path path, OpenOption... options) throws IOException {
            return Files.newBufferedWriter(path, options);
        }

        public BufferedReader newBufferedReader(Path path, Charset cs) throws IOException {
            return Files.newBufferedReader(path, cs);
        }

        public List<String> readAllLines(Path path) throws IOException {
            return Files.readAllLines(path);
        }

        public boolean isDirectory(Path path, LinkOption... options) {
            return Files.isDirectory(path, options);
        }

        public Path createDirectories(Path path, FileAttribute<?>... attrs) throws IOException {
            return Files.createDirectories(path, attrs);
        }

        public Stream<Path> list(Path dir) throws IOException {
            return Files.list(dir);
        }

        public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
            return Files.newInputStream(path, options);
        }

        public boolean exists(Path path, LinkOption... options) {
            return Files.exists(path, options);
        }

        public Path copy(Path source, Path target, CopyOption... options)
                throws IOException {
            return Files.copy(source, target, options);
        }

        public Path move(Path source, Path target, CopyOption... options)
                throws IOException {
            return Files.move(source, target, options);
        }

        public Path write(Path path, Iterable<? extends CharSequence> lines, OpenOption... options)
                throws IOException {
            return Files.write(path, lines, options);
        }
    }

    public static BufferedWriter newBufferedWriter(Path path, OpenOption... options) throws IOException {
        return instance.newBufferedWriter(path, options);
    }


    public static BufferedReader newBufferedReader(Path path, Charset cs) throws IOException {
        return instance.newBufferedReader(path, cs);
    }

    public static BufferedReader newBufferedReader(Path path) throws IOException {
        return newBufferedReader(path, StandardCharsets.UTF_8);
    }

    public static List<String> readAllLines(Path path) throws IOException {
        return instance.readAllLines(path);
    }

    public static boolean isDirectory(Path path, LinkOption... options) {
        return instance.isDirectory(path, options);
    }

    public static Path createDirectories(Path path, FileAttribute<?>... attrs) throws IOException {
        return instance.createDirectories(path, attrs);
    }

    public static Stream<Path> list(Path dir) throws IOException {
        return instance.list(dir);
    }

    public static InputStream newInputStream(Path path, OpenOption... options) throws IOException {
        return instance.newInputStream(path, options);
    }

    public static boolean exists(Path path, LinkOption... options) {
        return instance.exists(path, options);
    }

    public static Path copy(Path source, Path target, CopyOption... options)
            throws IOException {
        return instance.copy(source, target, options);
    }

    public static Path move(Path source, Path target, CopyOption... options)
            throws IOException {
        return instance.move(source, target, options);
    }

    public static Path write(Path path, Iterable<? extends CharSequence> lines, OpenOption... options)
            throws IOException {
        return instance.write(path, lines, options);
    }
}

