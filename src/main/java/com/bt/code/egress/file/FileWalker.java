package com.bt.code.egress.file;

import com.bt.code.egress.Config;
import com.bt.code.egress.read.FilePathMatcher;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
@Slf4j
public class FileWalker {
    @SneakyThrows
    public static void walkFileTreeWithFilters(Path rootPath, Config.MatchingSets[] filters,
                                               Consumer<Path> dirPostAction,
                                               Consumer<Path> fileAction) {
        FilePathMatchers filePathMatchers = FilePathMatchers.fromConfigs(Lists.newArrayList(filters));
        try (Stream<Path> paths = LocalFiles.list(rootPath)) {
            paths.forEach(path -> {
                if (LocalFiles.isDirectory(path)) {
                    if (filePathMatchers.match(path + "/")) {
                        walkFileTreeWithFilters(path, filters, dirPostAction, fileAction);
                        dirPostAction.accept(path);
                    } else {
                        log.info("Ignore folder: {}", path);
                    }
                    return;
                }

                if (!filePathMatchers.match(path.getFileName().toString())) {
                    log.info("Ignore file: {}", path);
                    return;
                }
                fileAction.accept(path);
            });
        }
    }

    @AllArgsConstructor
    private static class FilePathMatchers {
        private List<FilePathMatcher> matchers;

        public static FilePathMatchers fromConfigs(List<Config.MatchingSets> configs) {
            List<FilePathMatcher> matchers = configs.stream().map(FilePathMatcher::fromConfig)
                    .collect(Collectors.toList());
            FilePathMatchers result = new FilePathMatchers(matchers);
            return result;
        }

        public boolean match(String name) {
            return matchers.stream().map(m -> m.match(name)).allMatch(r -> r);
        }
    }
}
