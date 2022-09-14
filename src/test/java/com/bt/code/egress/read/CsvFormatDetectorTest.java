package com.bt.code.egress.read;

import org.apache.commons.csv.QuoteMode;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;

class CsvFormatDetectorTest {

    @Test
    void getQuoteModeNoComment() throws IOException {
        CsvFormatDetector csvFormatDetector = new CsvFormatDetector(',', '"', null);

        String LINE_1 = "1,John,new employee";
        assertThat(csvFormatDetector.getQuoteMode(new BufferedReader(new StringReader(LINE_1))))
                .as(LINE_1)
                .isEqualTo(QuoteMode.MINIMAL);

        String LINE_2 = "2,\"Mary Lee\",new employee";
        assertThat(csvFormatDetector.getQuoteMode(new BufferedReader(new StringReader(LINE_2))))
                .as(LINE_2)
                .isEqualTo(QuoteMode.MINIMAL);

        String LINE_3 = "3,\"Carl, Jr.\",new employee";
        assertThat(csvFormatDetector.getQuoteMode(new BufferedReader(new StringReader(LINE_3))))
                .as(LINE_3)
                .isEqualTo(QuoteMode.MINIMAL);

        String LINE_4 = ",,";
        assertThat(csvFormatDetector.getQuoteMode(new BufferedReader(new StringReader(LINE_4))))
                .as(LINE_4)
                .isEqualTo(QuoteMode.MINIMAL);

        String LINE_7 = ",\"\",";
        assertThat(csvFormatDetector.getQuoteMode(new BufferedReader(new StringReader(LINE_7))))
                .as(LINE_7)
                .isEqualTo(QuoteMode.MINIMAL);

        String LINE_8 = "\"a\",\"b\",\"c\"";
        assertThat(csvFormatDetector.getQuoteMode(new BufferedReader(new StringReader(LINE_8))))
                .as(LINE_8)
                .isEqualTo(QuoteMode.ALL);

        String LINE_9 = "\"a a\",\"b b\",\"c c\"";
        assertThat(csvFormatDetector.getQuoteMode(new BufferedReader(new StringReader(LINE_9))))
                .as(LINE_9)
                .isEqualTo(QuoteMode.ALL);

        String LINE_10 = "\"\",\"\",\"\"";
        assertThat(csvFormatDetector.getQuoteMode(new BufferedReader(new StringReader(LINE_10))))
                .as(LINE_10)
                .isEqualTo(QuoteMode.ALL);

        String LINE_12 = "\"a\",null,null,null,\"b\"";
        assertThat(csvFormatDetector.getQuoteMode(new BufferedReader(new StringReader(LINE_12))))
                .as(LINE_12)
                .isEqualTo(QuoteMode.ALL);
    }

    @Test
    void getQuoteModeWithComment() throws IOException {
        CsvFormatDetector csvFormatDetector = new CsvFormatDetector(',', '"', '#');

        String LINE_1 = "#\"a\",\"b\",\"c\"\n1,John,new employee";
        assertThat(csvFormatDetector.getQuoteMode(new BufferedReader(new StringReader(LINE_1))))
                .as(LINE_1)
                .isEqualTo(QuoteMode.MINIMAL);

        String LINE_2 = "#1,John,new employee\n\"a\",\"b\",\"c\"";
        assertThat(csvFormatDetector.getQuoteMode(new BufferedReader(new StringReader(LINE_2))))
                .as(LINE_2)
                .isEqualTo(QuoteMode.ALL);

        String LINE_3 = "#1,John,new employee";
        assertThat(csvFormatDetector.getQuoteMode(new BufferedReader(new StringReader(LINE_3))))
                .as(LINE_3)
                .isEqualTo(QuoteMode.ALL);
    }
}