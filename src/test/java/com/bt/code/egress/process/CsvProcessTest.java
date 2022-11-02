package com.bt.code.egress.process;

import com.bt.code.egress.Config;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class CsvProcessTest extends ProcessTestBase {
    Path samplePath = Paths.get("data/csv/customer.csv");

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();

        Config.CsvReplacementConfig csvReplacementConfig = config.getCsv();
        csvReplacementConfig.setEnabled(true);

        Config.CsvFileConfig csvFileConfig = new Config.CsvFileConfig();
        csvReplacementConfig.getFiles().add(csvFileConfig);
        csvFileConfig.setFilename("**/customer.csv");
        csvFileConfig.getDictionary().put("Mnemonic", "");
        csvFileConfig.getColumns().put("Name", "n{Id}");
    }


    @Test
    void simple() {
        String sampleText = "Id,Name,Mnemonic,Description\n" +
                "10,Batman,VIP_BAT,Favorite customer of Acme Corp";
        fileSystem.write(samplePath, sampleText);
        runScan();
        assertThat(fileSystem.readAllLines(samplePath)).isEqualTo(ImmutableList.of(
                "Id,Name,Mnemonic,Description",
                "10,n10,VIP_BAT,Favorite customer of w281027120 Corp"));

        // restore
        config.getScan().setDirection("restore");
        runScan();
        assertThat(fileSystem.readAllLines(samplePath)).isEqualTo(ImmutableList.of(
                "Id,Name,Mnemonic,Description",
                "10,n10,VIP_BAT,Favorite customer of Acme Corp"));
    }

    @Test
    void dictionaryCandidateNoTemplateReplaced() {
        String sampleText = "Id,Name,Mnemonic,Description\n" +
                "10,n10,w12345,Name mashed; Mnemonic masked\n" +
                "20,n20_alt,w12345_ALT,Name mashed and altered; Mnemonic masked and altered\n" +
                "20,nnn,abc,Name changed manually not guarded; Mnemonic new clean";
        fileSystem.write(samplePath, sampleText);
        fileSystem.write(config.getReplace().getInstruction().getFiles().iterator().next().toPath(),
                "Allow,Text,Context,File,Line,Replacement,Comment\n" +
                        "True,csv:**/customer.csv:name,No guarded words found,csv/customer.csv");

        runScan();
        assertThat(fileSystem.read(samplePath)).isEqualTo(sampleText);
        assertThat(fileSystem.readAllLines(config.getCsv().getDictionaryCandidate().toPath())).isEqualTo(ImmutableList.of(
                "Text,Replacement,Scope,Comment",
                "w12345,,dictionary,Ignore: restorable",
                "w12345_alt,,dictionary,Ignore: restorable",
                "abc,,dictionary,",
                "n20_alt,,template,",
                "nnn,,template,"));

        // restore
        config.getScan().setDirection("restore");
        runScan();
        assertThat(fileSystem.readAllLines(samplePath)).isEqualTo(ImmutableList.of(
                "Id,Name,Mnemonic,Description",
                "10,n10,<<NO_RESTORE:w12345>>,Name mashed; Mnemonic masked",
                "20,n20_alt,<<NO_RESTORE:w12345>>_ALT,Name mashed and altered; Mnemonic masked and altered",
                "20,nnn,abc,Name changed manually not guarded; Mnemonic new clean"));
    }

    @Test
    void dictionaryCandidateTemplateReplaced() {
        String sampleText = "Id,Name,Mnemonic,Description\n" +
                "10,n10,w12345,Name mashed; Mnemonic masked\n" +
                "20,n20_alt,w12345_ALT,Name mashed and altered; Mnemonic masked and altered\n" +
                "30,Mickey,Acme_VIP,Name new; Mnemonic new\n" +
                "40,Acme!,-,Name new guarded; Mnemonic new short";
        fileSystem.write(samplePath, sampleText);
        fileSystem.write(config.getReplace().getInstruction().getFiles().iterator().next().toPath(),
                "Allow,Text,Context,File,Line,Replacement,Comment\n" +
                        "True,csv:**/customer.csv:name,No guarded words found,csv/customer.csv");

        runScan();
        assertThat(fileSystem.readAllLines(samplePath)).isEqualTo(ImmutableList.of(
                "Id,Name,Mnemonic,Description",
                "10,n10,w12345,Name mashed; Mnemonic masked",
                "20,n20,w12345_ALT,Name mashed and altered; Mnemonic masked and altered",
                "30,n30,w281027120_VIP,Name new; Mnemonic new",
                "40,n40,-,Name new guarded; Mnemonic new short"));
        ImmutableList<String> dictionaryCandidates = ImmutableList.of(
                "Text,Replacement,Scope,Comment",
                "\"-\",,dictionary,Ignore: too short",
                "w12345,,dictionary,Ignore: restorable",
                "w12345_alt,,dictionary,Ignore: restorable");
        assertThat(fileSystem.readAllLines(config.getCsv().getDictionaryCandidate().toPath())).isEqualTo(dictionaryCandidates);

        // restore
        config.getScan().setDirection("restore");
        runScan();
        assertThat(fileSystem.readAllLines(samplePath)).isEqualTo(ImmutableList.of(
                "Id,Name,Mnemonic,Description",
                "10,n10,<<NO_RESTORE:w12345>>,Name mashed; Mnemonic masked",
                "20,n20,<<NO_RESTORE:w12345>>_ALT,Name mashed and altered; Mnemonic masked and altered",
                "30,n30,Acme_VIP,Name new; Mnemonic new",
                "40,n40,-,Name new guarded; Mnemonic new short"));
        assertThat(fileSystem.readAllLines(config.getCsv().getDictionaryCandidate().toPath())).isEqualTo(dictionaryCandidates);
    }
}
