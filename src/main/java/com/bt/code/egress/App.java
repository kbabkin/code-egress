package com.bt.code.egress;

import com.bt.code.egress.process.CsvFileReplacer;
import com.bt.code.egress.process.FileLocation;
import com.bt.code.egress.process.FolderReplacer;
import com.bt.code.egress.process.JobRunner;
import com.bt.code.egress.process.LineReplacer;
import com.bt.code.egress.process.TextFileReplacer;
import com.bt.code.egress.process.WordReplacer;
import com.bt.code.egress.read.FilePathMatcher;
import com.bt.code.egress.read.LineGuardIgnoreMatcher;
import com.bt.code.egress.read.ReportMatcher;
import com.bt.code.egress.report.ReportCollector;
import com.bt.code.egress.report.ReportHelper;
import com.bt.code.egress.report.ReportWriter;
import com.bt.code.egress.report.Stats;
import com.bt.code.egress.write.EmptyFolderWriter;
import com.bt.code.egress.write.FolderWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import java.io.File;

@SpringBootApplication
@Import(Config.class)
@Slf4j
public class App implements ApplicationRunner {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    // Path(".") was resolved as target/classes
    @Value("${read.folder}")
    File folder;
    @Value("${read.threads:10}")
    int readThreads;
    @Value("${write.inplace:false}")
    boolean writeInplace;
    @Value("${write.folder}")
    File writeFolder;
    @Value("${write.report}")
    File writeReport;
    @Value("${write.generatedReplacement}")
    File writeGeneratedReplacement;
    @Value("${replace.defaultTemplate}")
    String replaceDefaultTemplate;
    @Value("${scan.project}")
    String scanProject;
    @Value("${scan.config}")
    String scanConfig;

    @Value("${csv.delim:,}")
    String csvDelim;
    @Value("${csv.quote:\"}")
    String csvQuote;

    @Value("${context.keepLength:15}")
    int contextKeepLength;
    @Value("${context.minCompareLength:2}")
    int contextMinCompareLength;

    @Autowired
    Config config;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Scan project: {}, config: {}", scanProject, scanConfig);
        log.info("Scanning folder: {}, write inplace: {}", folder, writeInplace);

        long startedAt = System.currentTimeMillis();
        FilePathMatcher filePathMatcher = FilePathMatcher.fromConfig(config.read);
        LineGuardIgnoreMatcher lineMatcher = LineGuardIgnoreMatcher.fromConfigsOptimized(config.word);
        ReportHelper reportHelper = new ReportHelper(contextKeepLength, contextMinCompareLength);
        ReportMatcher reportMatcher = ReportMatcher.fromConfigs(reportHelper, config.getAllow().getReportFiles());
        WordReplacer wordReplacer = new WordReplacer(replaceDefaultTemplate);
        LineReplacer lineReplacer = new LineReplacer(lineMatcher, reportMatcher, wordReplacer);
        ReportCollector reportCollector = new ReportCollector(reportHelper);
        TextFileReplacer textFileReplacer = new TextFileReplacer(lineReplacer, reportCollector);
        CsvFileReplacer csvFileReplacer = new CsvFileReplacer(textFileReplacer, lineReplacer,
                reportMatcher, reportHelper, reportCollector, config.csv, csvDelim, csvQuote);
        FolderWriter folderWriter = writeInplace ? new FolderWriter(folder.toPath()) : new EmptyFolderWriter(writeFolder.toPath());
        FolderReplacer folderReplacer = new FolderReplacer(csvFileReplacer, filePathMatcher,
                reportMatcher.getAllowFilePathMatcher(), reportCollector, folderWriter);
        ReportWriter reportWriter = new ReportWriter(reportHelper, writeReport.toPath());
        JobRunner jobRunner = new JobRunner(readThreads);
        log.info("Configured in {} ms", System.currentTimeMillis() - startedAt);

        try {
            folderReplacer.replace(FileLocation.forFile(folder), jobRunner::submit);
            jobRunner.run();
            reportWriter.onReport(reportCollector.toReport());
            wordReplacer.saveGenerated(writeGeneratedReplacement.toPath());
        } finally {
            folderWriter.verify();
            Stats.dump();
            log.info("Processed in {} ms", System.currentTimeMillis() - startedAt);
        }
    }
}
