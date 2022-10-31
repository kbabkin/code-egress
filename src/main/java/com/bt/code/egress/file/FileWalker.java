package com.bt.code.egress.file;

import com.bt.code.egress.Config;
import com.bt.code.egress.read.FilePathMatcher;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;

@UtilityClass
@Slf4j
public class FileWalker {
    @SneakyThrows
    public static void walkFileTreeWithFilter(Path rootPath, Config.MatchingSets filter,
                                        Consumer<Path> dirPostAction,
                                        Consumer<Path> fileAction) {
        FilePathMatcher filePathMatcher = FilePathMatcher.fromConfig(filter);
        try (Stream<Path> paths = LocalFiles.list(rootPath)) {
            paths.forEach(path -> {
                if (LocalFiles.isDirectory(path)) {
                    if (filePathMatcher.match(path + "/")) {
                        walkFileTreeWithFilter(path, filter, dirPostAction, fileAction);
                        dirPostAction.accept(path);
                    } else {
                        log.info("Ignore folder: {}", path);
                    }
                    return;
                }

                if (!filePathMatcher.match(path.getFileName().toString())) {
                    log.info("Ignore file: {}", path);
                    return;
                }
                fileAction.accept(path);
            });
        }
    }
}
