package com.bt.code.egress.read;

import com.bt.code.egress.file.BufferedReaderUtil;
import com.bt.code.egress.process.FileLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.QuoteMode;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Slf4j
public class CsvFormatDetector {
    private final char delim;
    private final char quote;
    private final Character commentMarker;

    public QuoteMode guessQuoteMode(FileLocation file) throws IOException {
        return BufferedReaderUtil.doWithBufferedReader(file, this::getQuoteMode, StandardCharsets.ISO_8859_1);
    }

    QuoteMode getQuoteMode(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            if (commentMarker == null || line.charAt(0) != commentMarker) {
                return line.charAt(0) == quote ? QuoteMode.ALL : QuoteMode.MINIMAL;
            }
        }
        //Default
        return QuoteMode.ALL;
    }
}
