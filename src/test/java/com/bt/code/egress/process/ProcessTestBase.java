package com.bt.code.egress.process;

import com.bt.code.egress.App;
import com.bt.code.egress.Config;
import com.bt.code.egress.file.LocalFiles;
import com.bt.code.egress.report.Stats;
import com.bt.code.egress.write.FolderMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.nio.file.Paths;

public class ProcessTestBase {
    protected FolderMock fileSystem = new FolderMock();
    protected Config config = new Config();

    @BeforeEach
    protected void setUp() {
        LocalFiles.setInstance(fileSystem);
        fileSystem.createDirectories(Paths.get("config"));
        fileSystem.createDirectories(Paths.get("data/text"));
        fileSystem.createDirectories(Paths.get("data/csv"));
        fileSystem.createDirectories(Paths.get("target"));

        Config.ReadConfig readConfig = config.getRead();
        readConfig.setFolder(new File("data"));

        Config.WriteConfig writeConfig = config.getWrite();
        writeConfig.setFolder(new File("target/preview"));
        writeConfig.setInplace(true);

        Config.DirectionConfig replaceConfig = config.getReplace();
        replaceConfig.getFile().getGuard().getValues().add("**/");
        replaceConfig.getFile().getGuard().getValues().add("**/*");
        replaceConfig.getWord().getGuard().getValues().put("acme", "");
        replaceConfig.getWord().getGuard().getPatterns().put("\\w[\\w.-]+@\\w+(\\.\\w+)+", "u{hash}@mail.local");
        replaceConfig.setDefaultTemplate("w{hash}");
        replaceConfig.getInstruction().getFiles().add(new File("config/replace-instruction.csv"));
        replaceConfig.setReport(new File("target/replace-report.csv"));
        replaceConfig.setRestoreInstructionCumulative(new File("target/restore-instruction-cumulative.csv"));
        replaceConfig.setRestoreInstructionLast(new File("target/restore-instruction-last.csv"));
        replaceConfig.setGeneratedReplacement(new File("target/generated-replacement.csv"));

        Config.DirectionConfig restoreConfig = config.getRestore();
        restoreConfig.getFile().getGuard().getValues().add("**/");
        restoreConfig.getFile().getGuard().getValues().add("**/*");
        restoreConfig.getWord().getGuard().getPatterns().put("w\\d{3,}", "");
        restoreConfig.getWord().getGuard().getPatterns().put("h\\d{3,}.domain.local", "");
        restoreConfig.getWord().getGuard().getPatterns().put("u\\d{3,}@mail.local", "");
        restoreConfig.getInstruction().getFiles().add(replaceConfig.getRestoreInstructionCumulative());
        restoreConfig.setReport(new File("target/restore-report.csv"));
        restoreConfig.setGeneratedReplacement(new File("target/restore-generated-replacement.csv"));

        config.getCsv().setDictionaryCandidate(new File("target/csv-suspects.csv"));
    }

    protected void runScan() {
        Stats.reset();
        App.RunnerBuilder runnerBuilder = App.RunnerBuilder.of(config);
        runnerBuilder.submit(JobRunner.DIRECT_RUNNER);
        runnerBuilder.close();
        fileSystem.dump();
        Stats.dump();
    }

    @AfterEach
    protected void tearDown() {
        LocalFiles.setInstance(new LocalFiles.LocalFilesImpl());
    }

}
