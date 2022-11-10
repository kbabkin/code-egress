package com.bt.code.egress.process;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class ReplaceTest extends ProcessTestBase {
    Path sampleTextPath = Paths.get("data/text/sample-text.txt");

    @Test
    void unreviewed() {
        fileSystem.write(sampleTextPath, "Company: ACME");
        runScan();
        assertThat(fileSystem.read(sampleTextPath)).isEqualTo("Company: w281027120");
        assertThat(fileSystem.readAllLines(config.getReplace().getReport().toPath())).isEqualTo(ImmutableList.of(
                "Allow,Text,Context,File,Line,Replacement,Comment",
                ",acme,Company: ACME,text/sample-text.txt,1,w281027120,Generated for Value acme"));
        assertThat(fileSystem.readAllLines(config.getReplace().getGeneratedReplacement().toPath())).isEqualTo(ImmutableList.of(
                "acme,w281027120"));
    }

    @Test
    void truePositive_AnyContext() {
        fileSystem.write(sampleTextPath, "Company: ACME");
        fileSystem.write(Paths.get("config/replace-instruction.csv"), ImmutableList.of(
                "Allow,Text,Context,File,Line,Replacement,Comment",
                "false,acme"));
        runScan();
        assertThat(fileSystem.read(sampleTextPath)).isEqualTo("Company: w281027120");
        assertThat(fileSystem.readAllLines(config.getReplace().getReport().toPath())).isEqualTo(ImmutableList.of(
                "Allow,Text,Context,File,Line,Replacement,Comment",
                "false,acme,Company: ACME,text/sample-text.txt,1,w281027120,Generated for Value acme"));
        assertThat(fileSystem.readAllLines(config.getReplace().getGeneratedReplacement().toPath())).isEqualTo(ImmutableList.of(
                "acme,w281027120"));
    }

    @Test
    void falsePositive_InstructionCopiedFromScanReport() {
        fileSystem.write(sampleTextPath, "String acme = \"MyCompany\";");
        fileSystem.write(Paths.get("config/replace-instruction.csv"), ImmutableList.of(
                "Allow,Text,Context,File,Line,Replacement,Comment",
                "true,acme,\"String acme = \"\"MyCompany\"\";\",text/sample-text.txt,1,w281027120,Generated for Value acme"));
        runScan();
        assertThat(fileSystem.read(sampleTextPath)).isEqualTo("String acme = \"MyCompany\";");
        assertThat(fileSystem.readAllLines(config.getReplace().getReport().toPath())).isEqualTo(ImmutableList.of(
                "Allow,Text,Context,File,Line,Replacement,Comment",
                "true,acme,\"String acme = \"\"MyCompany\"\";\",text/sample-text.txt,1,,\"Allowed: Value acme, Suggested: w281027120\""));
    }

    @Test
    void falsePositive_CommonContext() {
        fileSystem.write(sampleTextPath, "String acme = \"MyCompany\";");
        fileSystem.write(Paths.get("config/replace-instruction.csv"), ImmutableList.of(
                "Allow,Text,Context,File,Line,Replacement,Comment",
                "true,acme,String acme"));
        runScan();
        assertThat(fileSystem.read(sampleTextPath)).isEqualTo("String acme = \"MyCompany\";");
        assertThat(fileSystem.readAllLines(config.getReplace().getReport().toPath())).isEqualTo(ImmutableList.of(
                "Allow,Text,Context,File,Line,Replacement,Comment",
                "true,acme,\"String acme = \"\"MyCompany\"\";\",text/sample-text.txt,1,,\"Allowed: Value acme, Suggested: w281027120\""));
    }

    @Test
    void falsePositive_FilePattern() {
        fileSystem.write(sampleTextPath, "String acme = \"MyCompany\";");
        fileSystem.write(Paths.get("config/replace-instruction.csv"), ImmutableList.of(
                "Allow,Text,Context,File,Line,Replacement,Comment",
                "true,acme,,**/sample*.txt"));
        runScan();
        assertThat(fileSystem.read(sampleTextPath)).isEqualTo("String acme = \"MyCompany\";");
        assertThat(fileSystem.readAllLines(config.getReplace().getReport().toPath())).isEqualTo(ImmutableList.of(
                "Allow,Text,Context,File,Line,Replacement,Comment",
                "true,acme,\"String acme = \"\"MyCompany\"\";\",text/sample-text.txt,1,,\"Allowed: Value acme, Suggested: w281027120\""));
    }

    @Test
    void widerContextWins() {
        fileSystem.write(sampleTextPath, "Company email: sales@acme.com");
        runScan();
        assertThat(fileSystem.read(sampleTextPath)).isEqualTo("Company email: u125255358@mail.local");
        assertThat(fileSystem.readAllLines(config.getReplace().getReport().toPath())).isEqualTo(ImmutableList.of(
                "Allow,Text,Context,File,Line,Replacement,Comment",
                ",sales@acme.com,Company email: sales@acme.com,text/sample-text.txt,1,u125255358@mail.local,Generated for Pattern \\w[\\w.-]+@\\w+(\\.\\w+)+"));
        assertThat(fileSystem.readAllLines(config.getReplace().getGeneratedReplacement().toPath())).isEqualTo(ImmutableList.of(
                "acme,w281027120",
                "sales@acme.com,u125255358@mail.local"));
    }

}
