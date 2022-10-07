package com.bt.code.egress.write;

import com.bt.code.egress.file.LocalFiles;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.CopyOption;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RequiredArgsConstructor
public class FolderMock extends LocalFiles.LocalFilesImpl {
    private final Map<String, FolderMock> folders = new TreeMap<>();
    private final Map<String, String> files = new TreeMap<>();
    @Getter
    private final Path ownPath;

    public FolderMock() {
        this.ownPath = null;
    }

    public FolderMock(Path parentPath, String name) {
        this.ownPath = parentPath == null ? Paths.get(name) : parentPath.resolve(name);
    }

    Path resolve(String other) {
        return ownPath == null ? Paths.get(other) : ownPath.resolve(other);
    }

    @Override
    public BufferedWriter newBufferedWriter(Path path, OpenOption... options) {
        StringWriter stringWriter = new StringWriter();
        return new BufferedWriter(stringWriter) {
            @Override
            public void close() throws IOException {
                super.close();
                FolderMock.this.write(path, stringWriter.toString());
            }
        };
    }

    @Override
    public BufferedReader newBufferedReader(Path path, Charset cs) {
        String content = read(path);
        return new BufferedReader(new StringReader(content));
    }

    @Override
    public List<String> readAllLines(Path path) {
        return Arrays.asList(read(path).split("[\\r\\n]+"));
    }

    FolderMock getDirectParent(Path path, boolean createMissing) {
        assertThat(path.getNameCount()).as("Empty path: %s", path).isGreaterThan(0);
        if (path.getNameCount() == 1) {
            return this;
        }
        String name = path.getName(0).getFileName().toString();
        FolderMock folder = folders.get(name);
        if (folder == null) {
            assertThat(createMissing).as("No folder: %s", path.getName(0)).isTrue();
            folder = createDirectory(name);
        }
        return folder.getDirectParent(path.subpath(1, path.getNameCount()), createMissing);
    }

    public String read(Path path) {
        FolderMock directParent = getDirectParent(path, false);
        String name = path.getFileName().toString();
        assertThat(directParent.folders.containsKey(name)).as("Folder, not file: %s", path).isFalse();
        String content = directParent.files.get(name);
        assertThat(content).as("No file: %s", path).isNotNull();
        return content;
    }

    public void write(Path path, String content) {
        FolderMock directParent = getDirectParent(path, false);
        String name = path.getFileName().toString();
        assertThat(directParent.folders.containsKey(name)).as("Folder, not file: %s", path).isFalse();
        directParent.files.put(name, content);
    }

    @Override
    public boolean isDirectory(Path path, LinkOption... options) {
        FolderMock directParent = getDirectParent(path, false);
        String name = path.getFileName().toString();
        return directParent.folders.containsKey(name);
    }

    FolderMock createDirectory(String name) {
        return folders.computeIfAbsent(name, n -> {
            assertThat(files.containsKey(name)).as("File already exists with same name: %s/%s", getOwnPath(), name).isFalse();
            return new FolderMock(FolderMock.this.getOwnPath(), name);
        });
    }

    @Override
    public Path createDirectories(Path path, FileAttribute<?>... attrs) {
        FolderMock directParent = getDirectParent(path, true);
        String name = path.getFileName().toString();
        return directParent.createDirectory(name).getOwnPath();
    }

    @Override
    public Stream<Path> list(Path path) {
        FolderMock directParent = getDirectParent(path, false);
        String name = path.getFileName().toString();
        FolderMock folder = directParent.folders.get(name);
        assertThat(folder).as("No folder: %s", path).isNotNull();
        return Stream.concat(folder.folders.values().stream()
                        .map(FolderMock::getOwnPath),
                folder.files.keySet().stream()
                        .map(folder::resolve));
    }

    @Override
    public InputStream newInputStream(Path path, OpenOption... options) {
        String content = read(path);
        return new ByteArrayInputStream(content.getBytes());
    }

    @Override
    public boolean exists(Path path, LinkOption... options) {
        FolderMock directParent = getDirectParent(path, false);
        String name = path.getFileName().toString();
        return directParent.folders.containsKey(name) || directParent.files.containsKey(name);
    }

    @Override
    public Path copy(Path source, Path target, CopyOption... options) {
        String content = read(source);
        write(target, content);
        return target;
    }

    @Override
    public Path move(Path source, Path target, CopyOption... options) {
        copy(source, target, options);
        getDirectParent(source, false).files.remove(source.getFileName().toString());
        return target;
    }

    @Override
    public Path write(Path path, Iterable<? extends CharSequence> lines, OpenOption... options) {
        write(path, String.join("\n", lines));
        return path;
    }

    public void dump() {
        List<String> list = new ArrayList<>();
        dump("", list);
        log.info("Tree:\n{}", String.join("\n", list));
    }

    private void dump(String prefix, List<String> results) {
        String subPrefix = "  " + prefix;
        for (FolderMock folder : folders.values()) {
            results.add(prefix + (folder.getOwnPath() == null ? "" : folder.getOwnPath().getFileName().toString()));
            folder.dump(subPrefix, results);
        }
        files.forEach((name, content) -> results.add(prefix + name + ":\n" +
                subPrefix + content.trim().replaceAll("[\\n\\r]+", "\n" + subPrefix)));
    }
}
