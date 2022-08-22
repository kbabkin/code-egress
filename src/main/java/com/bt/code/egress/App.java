package com.bt.code.egress;

import com.bt.code.egress.process.CsvFileReplacer;
import com.bt.code.egress.process.FileLocation;
import com.bt.code.egress.process.FileReplacer;
import com.bt.code.egress.process.FolderReplacer;
import com.bt.code.egress.process.LineReplacer;
import com.bt.code.egress.process.WordReplacer;
import com.bt.code.egress.read.LineGuardIgnoreMatcher;
import com.bt.code.egress.read.ReportMatcher;
import com.bt.code.egress.read.WordGuardIgnoreMatcher;
import com.bt.code.egress.report.ReportCollector;
import com.bt.code.egress.report.ReportHelper;
import com.bt.code.egress.report.ReportWriter;
import com.bt.code.egress.report.Stats;
import com.bt.code.egress.write.EmptyFolderWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import java.io.File;
import java.util.TreeMap;

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
    @Value("${write.folder}")
    File writeFolder;
    @Value("${write.report}")
    File writeReport;
    @Value("${write.generatedReplacement}")
    File writeGeneratedReplacement;
    @Value("${replace.defaultTemplate}")
    String replaceDefaultTemplate;

    @Autowired
    Config config;

    @Override
    public void run(ApplicationArguments args) {
        long startedAt = System.currentTimeMillis();
        WordGuardIgnoreMatcher fileMatcher = WordGuardIgnoreMatcher.fromConfigs(config.read);
        LineGuardIgnoreMatcher lineMatcher = LineGuardIgnoreMatcher.fromConfigsOptimized(config.word);
        ReportHelper reportHelper = new ReportHelper(15);
        ReportMatcher reportMatcher = ReportMatcher.fromConfigs(reportHelper, config.getAllow().getReportFiles());
        WordReplacer wordReplacer = new WordReplacer(replaceDefaultTemplate);
        LineReplacer lineReplacer = new LineReplacer(lineMatcher, reportMatcher, wordReplacer);
        ReportCollector reportCollector = new ReportCollector(reportHelper);
        FileReplacer fileReplacer = new FileReplacer(lineReplacer, reportCollector);
        EmptyFolderWriter fileCompletedListener = new EmptyFolderWriter(writeFolder.toPath());
        CsvFileReplacer csvFileReplacer = new CsvFileReplacer(config.csv);
        FolderReplacer folderReplacer = new FolderReplacer(fileReplacer, csvFileReplacer, fileMatcher, fileCompletedListener);
        ReportWriter reportWriter = new ReportWriter(reportHelper, writeReport.toPath());
        log.info("Configured in {} ms", System.currentTimeMillis() - startedAt);

        folderReplacer.replace(FileLocation.forFile(folder));
        reportWriter.onReport(reportCollector.toReport());

        wordReplacer.saveGenerated(writeGeneratedReplacement.toPath());

        log.info("Processed in {} ms, Counters: {}", System.currentTimeMillis() - startedAt, new TreeMap<>(Stats.getCounters()));
        fileCompletedListener.verify();
    }
}
