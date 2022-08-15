package com.bt.code.egress;

import com.bt.code.egress.process.*;
import com.bt.code.egress.read.GroupMatcher;
import com.bt.code.egress.read.LineMatcher;
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

    @Value("${folder}")
    File folder;

    @Autowired
    Config config;

    @Override
    public void run(ApplicationArguments args) {
        GroupMatcher fileMatcher = GroupMatcher.fromConfigs(config.file);
        LineMatcher lineMatcher = new LineMatcher(GroupMatcher.fromConfigs(config.word), null); //todo line ignore
        WordReplacer wordReplacer = WordReplacer.fromConfig(config.replace);
        LineReplacer lineReplacer = new LineReplacer(lineMatcher, wordReplacer);
        Picker picker = new Picker();
        FileReplacer fileReplacer = new FileReplacer(lineReplacer, picker::pick);
        FolderReplacer folderReplacer = new FolderReplacer(fileReplacer, fileMatcher);

        folderReplacer.replace(folder);
    }
}
