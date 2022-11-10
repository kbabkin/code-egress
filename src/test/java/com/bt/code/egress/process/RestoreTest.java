package com.bt.code.egress.process;

import com.bt.code.egress.Config;
import com.bt.code.egress.report.Stats;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RestoreTest extends ProcessTestBase {
    Path sampleTextPath = Paths.get("data/text/sample-text.txt");

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
        List<String> sampleText = ImmutableList.of(
                "Company: ACME",
                "Full name: Acme Corp");
        fileSystem.write(sampleTextPath, sampleText);
        runScan();
        List<String> replacedText = fileSystem.readAllLines(sampleTextPath);
        assertThat(replacedText).isEqualTo(ImmutableList.of(
                "Company: w281027120",
                "Full name: w281027120 Corp"));

        // restore
        config.getScan().setDirection("restore");
        fileSystem.write(sampleTextPath, ImmutableList.<String>builder().addAll(replacedText)
                .add("New usage: w281027120")
                .add("Non-existing: w0123456789").build());
        runScan();
        assertThat(fileSystem.readAllLines(sampleTextPath)).isEqualTo(ImmutableList.<String>builder().addAll(sampleText)
                .add("New usage: <<AMBIGUOUS:w281027120:ACME|Acme>>")
                .add("Non-existing: <<NO_RESTORE:w0123456789>>").build());
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

        String addedText = "Full name: Acme Corp";
        fileSystem.write(sampleTextPath, ImmutableList.of(replacedText, addedText));

        runScan();
        assertThat(fileSystem.readAllLines(sampleTextPath)).isEqualTo(ImmutableList.of(
                "Company: w281027120",
                "Full name: w281027120 Corp"));
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
        assertThat(fileSystem.readAllLines(sampleTextPath)).isEqualTo(ImmutableList.of(sampleText, addedText));
    }
}
