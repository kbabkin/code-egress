package com.bt.code.egress.read;

import com.bt.code.egress.Config;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvLineMatcherTest {

    private Config.Columns columnsConfig = createColumnsConfig();

    @Test
    void testGetMatches() {

        CsvLineMatcher csvLineMatcher = new CsvLineMatcher(columnsConfig, "id,name,email,address,description", ",", "\"", "test.csv");

        List<WordMatch> matches = csvLineMatcher.getMatches("1,Johny,johny@email.com,Philadelphia 1055,some user");

        assertEquals(1, matches.size());
        WordMatch match = matches.get(0);
        assertEquals(new LineToken("1,Johny,johny@email.com,Philadelphia 1055,some user", 2, 41), match.getLineToken());
        assertEquals("1,,N/A", match.getReplacement());
    }

    @Test
    void testMerge() {
        CsvLineMatcher csvLineMatcher = new CsvLineMatcher(columnsConfig, "id,name,email,address,description", ",", "\"", "test.csv");
        String line = "1,Joe,joe@server.com,\"100200 , NY\",somebody";

        assertEquals(Arrays.asList(new WordMatch(new LineToken(line, 2, 20), "A;B", null, "1,")),
                csvLineMatcher.merge(
                        Arrays.asList(
                                new WordMatch(new LineToken(line, 2, 5), "A", null, "1"),
                                new WordMatch(new LineToken(line, 6, 20), "B", null, "")
                        ),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        line)
        );
    }

    @Test
    void testApplyReplacements() {
        CsvLineMatcher csvLineMatcher = new CsvLineMatcher(columnsConfig, "id,name,email,address,description", ",", "\"", "test.csv");
        String line = "1,Joe,joe@server.com,\"100200 , NY\",somebody";
        WordMatch wm1 = new WordMatch(new LineToken(line, 2, 5), "A", null, "Nik");
        WordMatch wm2 = new WordMatch(new LineToken(line, 6, 20), "B", null, "");
        assertEquals("Nik,",
                csvLineMatcher.applyReplacements(line, Arrays.asList(wm1, wm2), 2, 20));
    }

    @Test
    void testValidateAndCleanup() {
        String line = "aaa,bbb,ccc,ddd";
        CsvLineMatcher csvLineMatcher = new CsvLineMatcher(columnsConfig, "id,name,email,address,description", ",", "\"", "test.csv");

        LinkedList<WordMatch> allMatchesSortedByStartPos1 = new LinkedList<>();
        WordMatch wm1 = new WordMatch(new LineToken(line, 0, 3), null, null, null);
        WordMatch wm2 = new WordMatch(new LineToken(line, 0, 3), null, null, null);
        WordMatch wm3 = new WordMatch(new LineToken(line, 2, 5), null, null, null);
        WordMatch wm4 = new WordMatch(new LineToken(line, 4, 7), null, null, null);
        allMatchesSortedByStartPos1.add(wm1);
        allMatchesSortedByStartPos1.add(wm2);
        allMatchesSortedByStartPos1.add(wm3);
        allMatchesSortedByStartPos1.add(wm4);

        csvLineMatcher.validateAndCleanup(allMatchesSortedByStartPos1);
        assertEquals(Arrays.asList(wm1, wm4), allMatchesSortedByStartPos1);

        LinkedList<WordMatch> allMatchesSortedByStartPos2 = new LinkedList<>();
        WordMatch wm5 = new WordMatch(new LineToken(line, 0, 3), null, null, null);
        WordMatch wm6 = new WordMatch(new LineToken(line, 4, 6), null, null, null);
        WordMatch wm7 = new WordMatch(new LineToken(line, 6, 8), null, null, null);
        WordMatch wm8 = new WordMatch(new LineToken(line, 9, 10), null, null, null);
        allMatchesSortedByStartPos2.add(wm5);
        allMatchesSortedByStartPos2.add(wm6);
        allMatchesSortedByStartPos2.add(wm7);
        allMatchesSortedByStartPos2.add(wm8);

        csvLineMatcher.validateAndCleanup(allMatchesSortedByStartPos2);
        assertEquals(Arrays.asList(wm5, wm6, wm7, wm8), allMatchesSortedByStartPos2);
    }

    private Config.Columns createColumnsConfig() {
        Config.Columns columnsConfig = new Config.Columns();
        columnsConfig.setReplace(ImmutableMap.of("name", "id"));
        columnsConfig.setClear(Arrays.asList("email"));
        columnsConfig.setFill(ImmutableMap.of("address", "N/A"));
        return columnsConfig;
    }
}