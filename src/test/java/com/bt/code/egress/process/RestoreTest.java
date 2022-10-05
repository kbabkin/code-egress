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
    }

    void runScan() {
        Stats.reset();
        App.RunnerBuilder runnerBuilder = App.RunnerBuilder.of(config);
        runnerBuilder.submit(JobRunner.DIRECT_RUNNER);
        runnerBuilder.close();
        fileSystem.dump();
        Stats.dump();
    }

    @AfterEach
    void tearDown() {
        LocalFiles.setInstance(new LocalFiles.LocalFilesImpl());
    }

    @Test
    void simple() {
        String sampleText = "Company: ACME";
        fileSystem.write(sampleTextPath, sampleText);
        runScan();
        String replacedText = fileSystem.read(sampleTextPath);
        assertThat(replacedText).isEqualTo("Company: w281027120");

        // restore
        config.getScan().setDirection("restore");
        runScan();
        assertThat(fileSystem.read(sampleTextPath)).isEqualTo(sampleText);
    }

    @Test
    void newOccurrences() {
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
        assertThat(fileSystem.read(sampleTextPath)).isEqualTo(sampleText +
                "\nNew usage: <<AMBIGUOUS:w281027120:ACME|Acme>>" +
                "\nNon-existing: <<NO_RESTORE:w0123456789>>");
        assertThat(fileSystem.readAllLines(config.getRestore().getGeneratedReplacement().toPath())).isEqualTo(ImmutableList.of(
                "w0123456789,<<NO_RESTORE:w0123456789>>",
                "w281027120,<<AMBIGUOUS:w281027120:ACME|Acme>>"));
    }

    @Test
    void restoreInstructionCumulative() {
        Config.DirectionConfig replaceConfig = config.getReplace();

        String sampleText = "Company: ACME";
        fileSystem.write(sampleTextPath, sampleText);
        runScan();
        String replacedText = fileSystem.read(sampleTextPath);
        assertThat(replacedText).isEqualTo(
                "Company: w281027120");
        assertThat(fileSystem.readAllLines(replaceConfig.getRestoreInstructionCumulative().toPath())).isEqualTo(ImmutableList.of(
                "Allow,Text,Context,File,Line,Replacement,Comment",
                ",w281027120,Company: w281027120,text/sample-text.txt,1,ACME,Restore Value acme"));

        String addedText = "\nFull name: Acme Corp";
        fileSystem.write(sampleTextPath, replacedText + addedText);

        runScan();
        assertThat(fileSystem.read(sampleTextPath)).isEqualTo(
                "Company: w281027120\n" +
                        "Full name: w281027120 Corp");
        assertThat(fileSystem.readAllLines(replaceConfig.getRestoreInstructionLast().toPath())).isEqualTo(ImmutableList.of(
                "Allow,Text,Context,File,Line,Replacement,Comment",
                ",w281027120,Full name: w281027120 Corp,text/sample-text.txt,2,Acme,Restore Value acme"));
        assertThat(fileSystem.readAllLines(replaceConfig.getRestoreInstructionCumulative().toPath())).isEqualTo(ImmutableList.of(
                "Allow,Text,Context,File,Line,Replacement,Comment",
                ",w281027120,Company: w281027120,text/sample-text.txt,1,ACME,Restore Value acme",
                ",w281027120,Full name: w281027120 Corp,text/sample-text.txt,2,Acme,Restore Value acme"));
        assertThat(Stats.get("Restore Report Lines - Last")).isEqualTo(1);
        assertThat(Stats.get("Restore Report Lines - Cumulative")).isEqualTo(2);

        // restore
        config.getScan().setDirection("restore");
        runScan();
        assertThat(fileSystem.read(sampleTextPath)).isEqualTo(sampleText + addedText);
    }
}
