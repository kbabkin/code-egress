package com.bt.code.egress;

import com.bt.code.egress.process.CsvFileReplacer;
import com.bt.code.egress.process.FileLocation;
import com.bt.code.egress.process.FileReplacer;
import com.bt.code.egress.process.FolderReplacer;
import com.bt.code.egress.process.JobRunner;
import com.bt.code.egress.process.LineReplacer;
import com.bt.code.egress.process.TextFileReplacer;
import com.bt.code.egress.process.WordReplacementGenerator;
import com.bt.code.egress.read.FilePathMatcher;
import com.bt.code.egress.read.InstructionMatcher;
import com.bt.code.egress.read.LineGuardIgnoreMatcher;
import com.bt.code.egress.report.Report;
import com.bt.code.egress.report.ReportCollector;
import com.bt.code.egress.report.ReportHelper;
import com.bt.code.egress.report.ReportWriter;
import com.bt.code.egress.report.RestoreReportWriter;
import com.bt.code.egress.report.Stats;
import com.bt.code.egress.write.EmptyFolderWriter;
import com.bt.code.egress.write.FolderWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

@SpringBootApplication
@Import(Config.class)
@Slf4j
public class App implements ApplicationRunner {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }


    @Autowired
    Config config;

    @Override
    public void run(ApplicationArguments args) {
        Config.ScanConfig scanConfig = config.getScan();
        log.info("Scan mode: {}, project: {}, config: {}", scanConfig.getScanMode(), scanConfig.getProject(), scanConfig.getConfig());
        log.info("Scanning folder: {}, write inplace: {}", config.getRead().getFolder(), config.getWrite().isInplace());

        long startedAt = System.currentTimeMillis();

        RunnerBuilder runnerBuilder = RunnerBuilder.of(config);
        JobRunner jobRunner = new JobRunner(config.getRead().getThreads());
        log.info("Configured in {} ms", System.currentTimeMillis() - startedAt);

        try {
            runnerBuilder.submit(jobRunner::submit);
            jobRunner.run();
            runnerBuilder.close();
        } finally {
            Stats.dump();
            log.info("Processed in {} ms", System.currentTimeMillis() - startedAt);
        }
    }

    @RequiredArgsConstructor
    public static class RunnerBuilder {
        final Config config;
        FolderReplacer folderReplacer;
        List<Runnable> closeListeners = new ArrayList<>();

        public void submit(BiConsumer<String, Runnable> submitter) {
            folderReplacer.replace(FileLocation.forFile(config.getRead().getFolder()), submitter);
        }

        public void close() {
            for (Runnable closeListener : closeListeners) {
                closeListener.run();
            }
        }

        public RunnerBuilder build() {
            ReportHelper reportHelper = createReportHelper();
            InstructionMatcher instructionMatcher = createInstructionMatcher(reportHelper);
            ReportCollector reportCollector = createReportCollector(reportHelper);
            LineReplacer lineReplacer = createLineReplacer(reportHelper, instructionMatcher, reportCollector);
            FileReplacer fileReplacer = createFileReplacer(lineReplacer, reportHelper, instructionMatcher, reportCollector);
            FolderWriter folderWriter = createFolderWriter();
            folderReplacer = createFolderReplacer(fileReplacer, lineReplacer, instructionMatcher, reportCollector, folderWriter);
            return this;
        }

        public static RunnerBuilder of(Config config) {
            return new RunnerBuilder(config).build();
        }

        public ReportHelper createReportHelper() {
            Config.ContextConfig contextConfig = config.getContext();
            return new ReportHelper(contextConfig.getKeepLength(), contextConfig.getMinCompareLength());
        }

        public ReportCollector createReportCollector(ReportHelper reportHelper) {
            ReportCollector reportCollector = new ReportCollector(reportHelper);
            ReportWriter reportWriter = new ReportWriter(reportHelper, config.getDirectionConfig().getReport().toPath());
            closeListeners.add(() -> reportWriter.onReport(reportCollector.toReport()));
            return reportCollector;
        }

        public InstructionMatcher createInstructionMatcher(ReportHelper reportHelper) {
            return InstructionMatcher.fromConfigs(reportHelper, config.getDirectionConfig().getInstruction().getFiles());
        }

        public LineReplacer createLineReplacer(ReportHelper reportHelper, InstructionMatcher instructionMatcher, ReportCollector reportCollector) {
            Config.DirectionConfig directionConfig = config.getDirectionConfig();
            LineGuardIgnoreMatcher lineMatcher = LineGuardIgnoreMatcher.fromConfigs(directionConfig.getWord(), instructionMatcher.getSimpleReplacements());
            WordReplacementGenerator wordReplacementGenerator = Config.ScanDirection.RESTORE.equals(config.getScan().getScanMode())
                    ? instructionMatcher.getRestoreWordReplacer()
                    : new WordReplacementGenerator(directionConfig.getDefaultTemplate());
            ReportCollector restoreInstructionCollector = Config.ScanDirection.RESTORE.equals(config.getScan().getScanMode())
                    ? null
                    : new ReportCollector(reportHelper);
            LineReplacer lineReplacer = new LineReplacer(lineMatcher, reportCollector, restoreInstructionCollector, instructionMatcher, wordReplacementGenerator);
            if (restoreInstructionCollector != null) {
                RestoreReportWriter restoreInstructionLastWriter = new RestoreReportWriter(reportHelper,
                        directionConfig.getRestoreInstructionLast().toPath(), "Last", Collections.emptyList());
                RestoreReportWriter restoreInstructionCumulativeWriter = RestoreReportWriter.fromCumulativeConfig(reportHelper,
                        directionConfig.getRestoreInstructionCumulative().toPath(), "Cumulative", config.getRestore().getInstruction().getFiles());
                closeListeners.add(() -> {
                    Report report = restoreInstructionCollector.toReport();
                    restoreInstructionLastWriter.onReport(report);
                    restoreInstructionCumulativeWriter.onReport(report);
                });
            }
            closeListeners.add(() -> wordReplacementGenerator.saveGenerated(directionConfig.getGeneratedReplacement().toPath()));
            return lineReplacer;
        }

        public FileReplacer createFileReplacer(LineReplacer lineReplacer, ReportHelper reportHelper,
                                               InstructionMatcher instructionMatcher, ReportCollector reportCollector) {
            TextFileReplacer textFileReplacer = new TextFileReplacer(lineReplacer);
            //todo for restore csv only non-template
            return new CsvFileReplacer(textFileReplacer, lineReplacer,
                    instructionMatcher, reportHelper, reportCollector, config.getCsv());
        }

        public FolderWriter createFolderWriter() {
            Config.WriteConfig writeConfig = config.getWrite();
            FolderWriter folderWriter = writeConfig.isInplace()
                    ? new FolderWriter(config.getRead().getFolder().toPath())
                    : new EmptyFolderWriter(writeConfig.getFolder().toPath());
            closeListeners.add(folderWriter::verify);
            return folderWriter;
        }

        public FolderReplacer createFolderReplacer(FileReplacer fileReplacer,
                                                   LineReplacer lineReplacer, InstructionMatcher instructionMatcher,
                                                   ReportCollector reportCollector, FolderWriter folderWriter) {
            FilePathMatcher filePathMatcher = FilePathMatcher.fromConfig(config.getDirectionConfig().getFile());
            return new FolderReplacer(fileReplacer, filePathMatcher,
                    instructionMatcher.getAllowFilePathMatcher(), lineReplacer, reportCollector,
                    folderWriter, folderWriter);
        }
    }
}
