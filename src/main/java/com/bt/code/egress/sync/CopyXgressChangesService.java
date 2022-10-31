package com.bt.code.egress.sync;

import com.bt.code.egress.Config;
import com.bt.code.egress.file.FileWalker;
import com.bt.code.egress.file.LocalFiles;
import com.bt.code.egress.util.ConfirmationUtil;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

@Slf4j
@RequiredArgsConstructor
@Service

public class CopyXgressChangesService {

    private final XgressSourceService xgressSourceService;

    private static final String LATEST_EGRESS_TAG = "EGRESS_LATEST";
    private static final String TIMESTAMPED_EGRESS_TAG_PREFIX = "EGRESS_";

    @SneakyThrows
    public void copyPrivateChanges(Config.CopyChangesConfig copyChangesConfig) {
        Config.CopyMode copyMode = copyChangesConfig.getPrivateSource().getMode();

        String source = copyChangesConfig.getPrivateSource().getDir();
        String target = copyChangesConfig.getPublicSource().getDir();
        xgressSourceService.checkDirectory(source, "source private repo");
        xgressSourceService.checkDirectory(target, "target private repo");

        xgressSourceService.checkBranchCheckout(source,
                copyChangesConfig.getPrivateSource().getEgress().getStaging().getName());
        xgressSourceService.checkBranchCheckout(target,
                copyChangesConfig.getPublicSource().getEgress().getBranch());

        switch (copyMode) {
            case FILES:
                copyFolder(source, target, copyChangesConfig.getFile());
                break;
            case GIT:
                //TODO clarify
                //copyGitChanges(source, target, copyChangesConfig.getPrivateSource().getEgress().getStaging().getName());
                break;
        }
    }

    @SneakyThrows
    public void copyPublicChanges(Config.CopyChangesConfig copyChangesConfig) {
        Config.CopyMode copyMode = copyChangesConfig.getPublicSource().getMode();

        String source = copyChangesConfig.getPublicSource().getDir();
        String target = copyChangesConfig.getPrivateSource().getDir();
        xgressSourceService.checkDirectory(source, "source public repo");
        xgressSourceService.checkDirectory(target, "target public repo");

        xgressSourceService.checkTagCheckout(source,
                copyChangesConfig.getPublicSource().getIngress().getTag(),
                copyChangesConfig.getPublicSource().getIngress().getBranch());
        xgressSourceService.checkBranchCheckout(target,
                copyChangesConfig.getPrivateSource().getIngress().getBranch());

        switch (copyMode) {
            case FILES:
                copyFolder(source, target, copyChangesConfig.getFile());
                break;
            case GIT:
                //TODO clarify
                //copyGitChanges(source, target, copyChangesConfig);
                break;
        }
    }


    @SneakyThrows
    private void copyFolder(String source, String target, Config.MatchingSets filter) {

        log.info("Copying folder contents: {} to {} with filter: {}", source, target, filter);

        boolean completed = cleanupTargetDir(target, filter);
        if (!completed) {
            return;
        }

        copyToTargetDir(source, target, filter);
        log.info("Completed copying folder contents: {} to {}.", source, target);
    }

    @SneakyThrows
    private boolean cleanupTargetDir(String target, Config.MatchingSets filter) {
        log.info("Starting cleanup of {}", target);

        List<Path> filesToDelete = Lists.newArrayList();
        List<Path> dirsToDelete = Lists.newArrayList();

        //cleanup target, excluding excludes
        Path targetPath = Paths.get(target);
        FileWalker.walkFileTreeWithFilter(targetPath, filter,
                (Path dirPath) -> {
                    dirsToDelete.add(dirPath);
                },
                (Path filePath) -> {
                    filesToDelete.add(filePath);
                });

        boolean okToDelete = ConfirmationUtil.confirm(
                String.format("%s file(s) and %s directories will be deleted in folder %s, total file size %s. " +
                                "Confirm deletion?",
                        filesToDelete.size(), dirsToDelete.size(), target, getTotalSize(dirsToDelete)));
        if (!okToDelete) {
            log.warn("cleanupTargetDir aborted by user.");
            return false;
        }

        for (Path fileToDelete : filesToDelete) {
            if (Files.exists(fileToDelete)) {
                Files.delete(fileToDelete);
            } else {
                throw new RuntimeException(String.format("File %s does not exist and cannot be deleted", fileToDelete));
            }
        }

        for (Path dirToDelete : dirsToDelete) {
            if (Files.exists(dirToDelete)) {
                Files.delete(dirToDelete);
            } else {
                throw new RuntimeException(String.format("Directory %s does not exist and cannot be deleted", dirToDelete));
            }
        }
        return true;
    }

    @SneakyThrows
    private void copyToTargetDir(String source, String target, Config.MatchingSets filter) {
        //copy from source to target, excluding items per configuration
        Path sourcePath = Paths.get(source);
        Path targetPath = Paths.get(target);

        FileWalker.walkFileTreeWithFilter(sourcePath, filter, (Path path) -> {},
                (Path path) -> {
                    Path relativePath = path.relativize(sourcePath);
                    Path pathInTarget = targetPath.resolve(relativePath);
                    try {
                        LocalFiles.createDirectories(pathInTarget.getParent());
                        LocalFiles.copy(path, pathInTarget);
                    } catch (IOException ie) {
                        throw new RuntimeException(String.format("Failed to copy %s to %s", sourcePath, pathInTarget), ie);
                    }
                });
    }


    @SneakyThrows
    public void copyGitChanges(String source, String target, String branch) {
        List<ChangeAction> gitChanges = xgressSourceService.getXgressChanges(source, branch, LATEST_EGRESS_TAG);
        copyChanges(source, target, gitChanges);
    }

    @SneakyThrows
    public void copyChanges(String source, String target, List<ChangeAction> changes) {
        log.info("Processing {} change(s) {} -> {}", changes.size(), source, target);

        int changeIndex = 0;
        for (ChangeAction change : changes) {
            Consumer<String> handler = getHandler(change.getType(), source, target);
            log.info("appying change #{}: {}", changeIndex, change);
            handler.accept(change.getPath());
            changeIndex++;
        }

        log.info("all {} change(s) applied", changes.size());

        log.info("=========================");
        log.info("files copied:");
        logChanges(changes, ChangeAction.ChangeType.COPY);
        log.info("files deleted:");
        logChanges(changes, ChangeAction.ChangeType.DELETE);
    }

    public void completePrivateChanges(Config.SourceConfig privateSourceConfig) {
        xgressSourceService.completeEgress(
                privateSourceConfig.getDir(),
                privateSourceConfig.getEgress().getBranch(),
                LATEST_EGRESS_TAG,
                TIMESTAMPED_EGRESS_TAG_PREFIX
                );
    }

    private Consumer<String> getHandler(ChangeAction.ChangeType type, String source, String target) {
        switch (type) {
            case COPY:
                return path -> copy(path, source, target);
            case DELETE:
                return path -> delete(path, source, target);
            default:
                checkArgument(false, "unknown change type: %s", type);
                return null; // unreachable
        }
    }

    @SneakyThrows
    private void copy(String path, String source, String target) {
        Path sourcePath = Paths.get(source, path);
        checkArgument(!LocalFiles.isDirectory(sourcePath), "found changed directory %s", sourcePath);

        Path targetPath = Paths.get(target, path);
        Path targetParent = targetPath.getParent();
        checkState(!Files.exists(targetParent) || Files.isDirectory(targetParent), "parent path %s exists and it is not directory", targetParent);

        if (!Files.exists(targetParent)) {
            Files.createDirectories(targetParent);
            log.info("directory {} created", targetParent);
        }

        LocalFiles.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("file {} copied to destination", sourcePath);
    }

    @SneakyThrows
    private void delete(String path, String source, String target) {
        Path targetPath = Paths.get(target, path);
        if (!Files.exists(targetPath)) {
            log.warn("file {} not found in target", path);
            return;
        }

        Files.delete(targetPath);
        log.info("file {} deleted", targetPath);
    }

    private void logChanges(List<ChangeAction> changes, ChangeAction.ChangeType type) {
        changes.stream()
                .filter(it -> it.getType() == type)
                .forEach(it -> log.info("- {}", it.getPath()));
    }

    private long getTotalSize(List<Path> files) {
        return files.stream().mapToLong(path -> {
            long size = 0;
            try {
                size = Files.size(path);
            } catch (IOException e) {
                log.error("Failed to determine size of {}", path, e);
            }
            return size;
        }).sum();
    }
}

