package com.bt.code.egress;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.bt.code.egress.sync.ChangeAction;
import com.bt.code.egress.sync.ChangeAction.ChangeType;
import com.bt.code.egress.sync.EgressSourceService;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

// @SpringBootApplication
@Slf4j
@RequiredArgsConstructor
public class CopyEgressChangesApp implements ApplicationRunner {
    public static void main(String[] args) {
        // System.setProperty("egress-source", "/Users/vpa/projects/airflow-vpa");
        // System.setProperty("egress-target", "/Users/vpa/work/code-egress/target/vpa");

        // SpringApplication.run(CopyEgressChangesApp.class, args);
    }

    @Value("${egress-source}")
    private final String source;

    @Value("${egress-target}")
    private final String target;

    private final EgressSourceService egressSource;

    @Override
    @SneakyThrows
    public void run(ApplicationArguments args) {
        Path sourcePath = Paths.get(source);
        checkArgument(Files.isDirectory(sourcePath), "specified source repo (%s) is not directory", source);

        Path targetPath = Paths.get(target);
        checkArgument(Files.isDirectory(targetPath), "specified target repo (%s) is not directory", target);

        List<ChangeAction> changes = egressSource.getEgressChanges(source);
        log.info("{} change(s) should be applied to target", changes.size());

        int changeIndex = 0;
        for (ChangeAction change : changes) {
            Consumer<String> handler = getHandler(change.getType());
            log.info("appying change #{}: {}", changeIndex, change);
            handler.accept(change.getPath());
            changeIndex++;
        }

        log.info("all {} change(s) applied", changes.size());

        egressSource.completeEgress(source);
        log.info("egress changes copied to destination");

        log.info("=========================");
        log.info("files copied:");
        logChanges(changes, ChangeType.COPY);
        log.info("files deleted:");
        logChanges(changes, ChangeType.DELETE);
    }

    private Consumer<String> getHandler(ChangeAction.ChangeType type) {
        switch (type) {
            case COPY: return this::copy;
            case DELETE: return this::delete;
            default: 
                checkArgument(false, "unknown change type: %s", type);
                return null; // unreachable
        }
    }

    @SneakyThrows
    private void copy(String path) {
        Path sourcePath = Paths.get(source, path);
        checkArgument(!Files.isDirectory(sourcePath), "found changed directory %s", sourcePath);

        Path targePath = Paths.get(target, path);
        Path targetParent = targePath.getParent();
        checkState(!Files.exists(targetParent) || Files.isDirectory(targetParent), "parent path %s exists and it is not directory", targetParent);
    
        if (!Files.exists(targetParent)) {
            Files.createDirectories(targetParent);
            log.info("directory {} created", targetParent);
        }

        Files.copy(sourcePath, targePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("file {} copied to desination", sourcePath);
    }

    @SneakyThrows
    private void delete(String path) {
        Path targePath = Paths.get(target, path);
        if (!Files.exists(targePath)) {
            log.warn("file {} not found in target", path);
            return;
        }

        Files.delete(targePath);
        log.info("file {} deleted", targePath);
    }

    private void logChanges(List<ChangeAction> changes, ChangeType type) {
        changes.stream()
            .filter(it -> it.getType() == type)
            .forEach(it -> log.info("- {}", it.getPath()));
    }
}
