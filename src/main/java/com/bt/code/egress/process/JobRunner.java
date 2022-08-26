package com.bt.code.egress.process;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

@RequiredArgsConstructor
@Slf4j
public class JobRunner {
    private final int readThreads;

    public final static BiConsumer<String, Runnable> DIRECT_RUNNER = JobRunner::execute;

    private final Map<String, Runnable> tasks = new LinkedHashMap<>();

    public void submit(String path, Runnable runnable) {
        Runnable prev = tasks.put(path, runnable);
        if (prev != null) {
            log.error("Duplicate task for {}", path);
        }
    }

    static String execute(String path, Runnable runnable) {
        log.info("Start {}", path);
        try {
            runnable.run();
        } catch (RuntimeException e) {
            log.error("Job error", e);
            throw e;
        }
        log.info("Finish {}", path);
        return path;
    }

    public void run() {
        ExecutorService executorService = Executors.newFixedThreadPool(readThreads);
        try {
            ExecutorCompletionService<String> completionService = new ExecutorCompletionService<>(executorService);
            tasks.forEach((path, runnable) -> completionService.submit(() -> execute(path, runnable)));

            for (int i = 0; i < tasks.size(); i++) {
                Future<String> take = completionService.take();
                try {
                    log.info("Done {}", take.get());
                } catch (InterruptedException e) {
                    log.info("Interrupted", e);
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    log.info("Done with exception", e.getCause());
                } catch (Exception e) {
                    log.info("Done with exception", e);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            log.info("Shutdown ExecutorService");
            executorService.shutdown();
        }
    }
}
