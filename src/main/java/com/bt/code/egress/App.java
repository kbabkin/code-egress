package com.bt.code.egress;

import com.bt.code.egress.process.CsvFileReplacer;
import com.bt.code.egress.process.FileLocation;
import com.bt.code.egress.process.FileReplacer;
import com.bt.code.egress.process.FolderReplacer;
import com.bt.code.egress.process.LineReplacer;
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
import java.util.TreeMap;
import java.util.stream.Collectors;

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

    @Autowired
    Config config;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Scan project: {}, config: {}", scanProject, scanConfig);
        log.info("Scanning folder: {}, write inplace: {}", folder, writeInplace);

        long startedAt = System.currentTimeMillis();
        FilePathMatcher filePathMatcher = FilePathMatcher.fromConfig(config.read);
        LineGuardIgnoreMatcher lineMatcher = LineGuardIgnoreMatcher.fromConfigsOptimized(config.word);
        ReportHelper reportHelper = new ReportHelper(15);
        ReportMatcher reportMatcher = ReportMatcher.fromConfigs(reportHelper, config.getAllow().getReportFiles());
        WordReplacer wordReplacer = new WordReplacer(replaceDefaultTemplate);
        LineReplacer lineReplacer = new LineReplacer(lineMatcher, reportMatcher, wordReplacer);
        ReportCollector reportCollector = new ReportCollector(reportHelper);
        FileReplacer fileReplacer = new FileReplacer(lineReplacer, reportCollector, config.csv, csvDelim, csvQuote);
        FolderWriter folderWriter = writeInplace ? new FolderWriter(folder.toPath()) : new EmptyFolderWriter(writeFolder.toPath());
        CsvFileReplacer csvFileReplacer = new CsvFileReplacer(config.csv);
        FolderReplacer folderReplacer = new FolderReplacer(fileReplacer, csvFileReplacer, filePathMatcher, folderWriter);
        ReportWriter reportWriter = new ReportWriter(reportHelper, writeReport.toPath());
        log.info("Configured in {} ms", System.currentTimeMillis() - startedAt);

        folderReplacer.replace(FileLocation.forFile(folder));
        reportWriter.onReport(reportCollector.toReport());

        wordReplacer.saveGenerated(writeGeneratedReplacement.toPath());

        log.info("Counters: \n\t{}", new TreeMap<>(Stats.getCounters()).entrySet().stream()
                .map(String::valueOf).collect(Collectors.joining("\n\t")));
        log.info("Processed in {} ms", System.currentTimeMillis() - startedAt);
        folderWriter.verify();
    }
}
