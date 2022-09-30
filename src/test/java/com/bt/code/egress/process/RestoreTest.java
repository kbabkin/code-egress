package com.bt.code.egress.process;

import com.bt.code.egress.App;
import com.bt.code.egress.Config;
import com.bt.code.egress.file.LocalFiles;
import com.bt.code.egress.report.Stats;
import com.bt.code.egress.write.FolderMock;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class RestoreTest {
    FolderMock fileSystem = new FolderMock();
    Config config = new Config();
    Path sampleTextPath = Paths.get("data/text/sample-text.txt");

    @BeforeEach
    void setUp() {
        LocalFiles.setInstance(fileSystem);
        fileSystem.createDirectories(sampleTextPath.getParent());
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
        replaceConfig.setReport(new File("target/replace-report.csv"));
        replaceConfig.setRestoreInstructionDraft(new File("target/restore-instruction-draft.csv"));
        replaceConfig.setGeneratedReplacement(new File("target/generated-replacement.csv"));

        Config.DirectionConfig restoreConfig = config.getRestore();
        restoreConfig.getFile().getGuard().getValues().add("**/");
        restoreConfig.getFile().getGuard().getValues().add("**/*");
        restoreConfig.getWord().getGuard().getPatterns().put("w\\d{3,}", "");
        restoreConfig.getWord().getGuard().getPatterns().put("h\\d{3,}.domain.local", "");
        restoreConfig.getWord().getGuard().getPatterns().put("u\\d{3,}@mail.local", "");
        restoreConfig.getInstruction().getFiles().add(replaceConfig.getRestoreInstructionDraft());
        restoreConfig.setReport(new File("target/restore-report.csv"));
        restoreConfig.setGeneratedReplacement(new File("target/restore-generated-replacement.csv"));
    }

    void runScan() {
        App.RunnerBuilder runnerBuilder = App.RunnerBuilder.of(config);
        runnerBuilder.submit(JobRunner.DIRECT_RUNNER);
        runnerBuilder.close();
        fileSystem.dump();
    }

    @AfterEach
    void tearDown() {
        LocalFiles.setInstance(new LocalFiles.LocalFilesImpl());
    }
        @Test
    void testUpperLower() {
        String sampleText = "Company: ACME" +
                "\nFull name: Acme Corp";
        fileSystem.write(sampleTextPath, sampleText);
        runScan();
        String replacedText = fileSystem.read(sampleTextPath);
        assertThat(replacedText).isEqualTo(
                "Company: w281027120\n" +
                        "Full name: w281027120 Corp");

        // restore
        config.getScan().setDirection("restore");
        fileSystem.write(sampleTextPath, replacedText +
                "\nNew usage: w281027120" +
                "\nNon-existing: w0123456789");
        runScan();
        Stats.dump();
        assertThat(fileSystem.read(sampleTextPath)).isEqualTo(sampleText +
                "\nNew usage: <<AMBIGUOUS:w281027120:ACME|Acme>>" +
                "\nNon-existing: <<NO_RESTORE:w0123456789>>");
        assertThat(fileSystem.readAllLines(config.getRestore().getGeneratedReplacement().toPath())).isEqualTo(ImmutableList.of(
                "w0123456789,<<NO_RESTORE:w0123456789>>",
                "w281027120,<<AMBIGUOUS:w281027120:ACME|Acme>>"));
    }
}
