package com.bt.code.egress.read;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CsvParserTest {

    @Test
    void readCsvLineAsStrings() {
    }

    @Test
    void testReadCsvLine() {
        CsvParser csvParser = new CsvParser("id,name,description", ",", "\"");

        String[] result1 = csvParser.readCsvLineAsStrings("1,John,new employee");
        assertArrayEquals(new String[]{"1", "John", "new employee"}, result1);

        String[] result2 = csvParser.readCsvLineAsStrings("2,\"Mary Lee\",new employee");
        assertArrayEquals(new String[]{"2", "\"Mary Lee\"", "new employee"}, result2);

        String[] result3 = csvParser.readCsvLineAsStrings("3,\"Carl, Jr.\",new employee");
        assertArrayEquals(new String[]{"3", "\"Carl, Jr.\"", "new employee"}, result3);

        String[] result4 = csvParser.readCsvLineAsStrings(",,");
        assertArrayEquals(new String[]{"", "", ""}, result4);

        String[] result5 = csvParser.readCsvLineAsStrings("5,,");
        assertArrayEquals(new String[]{"5", "", ""}, result5);

        String[] result6 = csvParser.readCsvLineAsStrings(",,rubbish");
        assertArrayEquals(new String[]{"", "", "rubbish"}, result6);

        String[] result7 = csvParser.readCsvLineAsStrings(",\"\",");
        assertArrayEquals(new String[]{"", "\"\"", ""}, result7);

    }

    @Test
    void testGetColumnIndex() {
        CsvParser csvParser1 = new CsvParser("id,name,description", ",", "\"");
        assertEquals(0, csvParser1.getColumnIndex("id"));
        assertEquals(1, csvParser1.getColumnIndex("name"));
        assertEquals(2, csvParser1.getColumnIndex("description"));

        CsvParser csvParser2 = new CsvParser("\"id\",\"name\",\"description\"", ",", "\"");
        assertEquals(0, csvParser2.getColumnIndex("id"));
        assertEquals(1, csvParser2.getColumnIndex("name"));
        assertEquals(2, csvParser2.getColumnIndex("description"));
    }
}