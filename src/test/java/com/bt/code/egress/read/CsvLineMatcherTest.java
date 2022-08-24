package com.bt.code.egress.read;

import com.bt.code.egress.Config;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
        //TODO implement
    }

    @Test
    void testApplyReplacements() {
        //TODO implement
    }

    private Config.Columns createColumnsConfig() {
        Config.Columns columnsConfig = new Config.Columns();
        columnsConfig.setReplace(ImmutableMap.of("name", "id"));
        columnsConfig.setClear(Arrays.asList("email"));
        columnsConfig.setFill(ImmutableMap.of("address", "N/A"));
        return columnsConfig;
    }
}