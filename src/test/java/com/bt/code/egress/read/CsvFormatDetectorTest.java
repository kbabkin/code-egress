package com.bt.code.egress.read;

import com.google.common.collect.Lists;
import org.apache.commons.csv.QuoteMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvFormatDetectorTest {

    @Test
    void testReadCsvLineAndGetQuoteMode() {
        CsvFormatDetector csvFormatDetector = new CsvFormatDetector(',', '"');

        String LINE_1 = "1,John,new employee";
        List<String> columns1 = csvFormatDetector.readCsvLine(LINE_1);
        assertEquals(Lists.newArrayList("1", "John", "new employee"), columns1);
        assertEquals(QuoteMode.MINIMAL, csvFormatDetector.guessQuoteModeByLines(LINE_1));

        String LINE_2 = "2,\"Mary Lee\",new employee";
        List<String> columns2 = csvFormatDetector.readCsvLine(LINE_2);
        assertEquals(Lists.newArrayList("2", "\"Mary Lee\"", "new employee"), columns2);
        assertEquals(QuoteMode.MINIMAL, csvFormatDetector.guessQuoteModeByLines(LINE_2));

        String LINE_3 = "3,\"Carl, Jr.\",new employee";
        List<String> columns3 = csvFormatDetector.readCsvLine(LINE_3);
        assertEquals(Lists.newArrayList("3", "\"Carl, Jr.\"", "new employee"), columns3);
        assertEquals(QuoteMode.MINIMAL, csvFormatDetector.guessQuoteModeByLines(LINE_3));

        String LINE_4 = ",,";
        List<String> columns4 = csvFormatDetector.readCsvLine(LINE_4);
        assertEquals(Lists.newArrayList("", "", ""), columns4);
        assertEquals(QuoteMode.ALL, csvFormatDetector.guessQuoteModeByLines(LINE_4));

        String LINE_5 = "5,,";
        List<String> columns5 = csvFormatDetector.readCsvLine(LINE_5);
        assertEquals(Lists.newArrayList("5", "", ""), columns5);
        assertEquals(QuoteMode.MINIMAL, csvFormatDetector.guessQuoteModeByLines(LINE_5));

        String LINE_6 = ",,rubbish";
        List<String> columns6 = csvFormatDetector.readCsvLine(LINE_6);
        assertEquals(Lists.newArrayList("", "", "rubbish"), columns6);
        assertEquals(QuoteMode.MINIMAL, csvFormatDetector.guessQuoteModeByLines(LINE_6));

        String LINE_7 = ",\"\",";
        List<String> columns7 = csvFormatDetector.readCsvLine(LINE_7);
        assertEquals(Lists.newArrayList("", "\"\"", ""), columns7);
        assertEquals(QuoteMode.ALL, csvFormatDetector.guessQuoteModeByLines(LINE_7));

        String LINE_8 = "\"a\",\"b\",\"c\"";
        List<String> columns8 = csvFormatDetector.readCsvLine(LINE_8);
        assertEquals(Lists.newArrayList("\"a\"", "\"b\"", "\"c\""), columns8);
        assertEquals(QuoteMode.ALL, csvFormatDetector.guessQuoteModeByLines(LINE_8));

        String LINE_9 = "\"a a\",\"b b\",\"c c\"";
        List<String> columns9 = csvFormatDetector.readCsvLine(LINE_9);
        assertEquals(Lists.newArrayList("\"a a\"", "\"b b\"", "\"c c\""), columns9);
        assertEquals(QuoteMode.ALL, csvFormatDetector.guessQuoteModeByLines(LINE_9));

        String LINE_10 = "\"\",\"\",\"\"";
        List<String> columns10 = csvFormatDetector.readCsvLine(LINE_10);
        assertEquals(Lists.newArrayList("\"\"", "\"\"", "\"\""), columns10);
        assertEquals(QuoteMode.ALL, csvFormatDetector.guessQuoteModeByLines(LINE_10));

        String LINE_11 = "\"\"";
        List<String> columns11 = csvFormatDetector.readCsvLine(LINE_11);
        assertEquals(Lists.newArrayList("\"\""), columns11);
        assertEquals(QuoteMode.ALL, csvFormatDetector.guessQuoteModeByLines(LINE_11));

        String LINE_12 = "\"a\",null,null,null,\"b\"";
        List<String> columns12 = csvFormatDetector.readCsvLine(LINE_12);
        assertEquals(Lists.newArrayList("\"a\"", "", "", "", "\"b\""), columns12);
        assertEquals(QuoteMode.ALL, csvFormatDetector.guessQuoteModeByLines(LINE_12));

        assertEquals(QuoteMode.ALL, csvFormatDetector.guessQuoteModeByLines(LINE_10, LINE_11));
        assertEquals(QuoteMode.ALL, csvFormatDetector.guessQuoteModeByLines(LINE_8, LINE_9));
        assertEquals(QuoteMode.ALL, csvFormatDetector.guessQuoteModeByLines(LINE_8, LINE_9, LINE_10, LINE_11));
        assertEquals(QuoteMode.MINIMAL, csvFormatDetector.guessQuoteModeByLines(LINE_5, LINE_6, LINE_10));
    }
}