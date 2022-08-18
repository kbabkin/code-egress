package com.bt.code.egress;

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
import com.bt.code.egress.write.EmptyFolderWriter;
import com.bt.code.egress.write.FileCompleted;
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

    @Autowired
    Config config;

    @Override
    public void run(ApplicationArguments args) {
        WordGuardIgnoreMatcher fileMatcher = WordGuardIgnoreMatcher.fromConfigs(config.read);
        LineGuardIgnoreMatcher lineMatcher = LineGuardIgnoreMatcher.fromConfigs(config.word);
        ReportHelper reportHelper = new ReportHelper(15);
        ReportMatcher reportMatcher = ReportMatcher.fromConfigs(reportHelper, config.getAllow().getReportFiles());
        WordReplacer wordReplacer = WordReplacer.fromConfig(config.replace);
        LineReplacer lineReplacer = new LineReplacer(lineMatcher, reportMatcher, wordReplacer);
        ReportCollector reportCollector = new ReportCollector(reportHelper);
        FileReplacer fileReplacer = new FileReplacer(lineReplacer, reportCollector);
        FileCompleted.Listener fileCompletedListener = new EmptyFolderWriter(writeFolder.toPath());
        FolderReplacer folderReplacer = new FolderReplacer(fileReplacer, fileMatcher, fileCompletedListener);
        ReportWriter reportWriter = new ReportWriter(reportHelper, writeReport.toPath());

        folderReplacer.replace(folder.toPath());
        reportWriter.onReport(reportCollector.toReport());
    }
}
