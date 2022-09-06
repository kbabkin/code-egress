package com.bt.code.egress.read;

import com.bt.code.egress.process.BufferedReaderUtil;
import com.bt.code.egress.process.CsvUtil;
import com.bt.code.egress.process.FileLocation;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class CsvFormatDetector {
    private final char delim;
    private final char quote;

    public CsvFormatDetector(char delim, char quote) {
        this.delim = delim;
        this.quote = quote;
    }

    public QuoteMode guessQuoteMode(FileLocation file) throws IOException {
        BufferedReaderUtil.BufferedReaderFunction<QuoteMode> guesser = br -> {
                String headerLine = br.readLine();
                if (headerLine != null) {
                    String firstLine = br.readLine();
                    if (firstLine != null) {
                        //Guess by header and first line
                        return guessQuoteModeByLines(headerLine, firstLine);
                    } else {
                        //Guess by header only
                        return guessQuoteModeByLines(headerLine);
                    }
                }
            //Default
            return QuoteMode.ALL;
        };
        return BufferedReaderUtil.doWithBufferedReader(file, guesser, StandardCharsets.ISO_8859_1);
    }

    //If all fields are quoted or empty, then use QUOTED mode.
    //Else it means that there is at least one unquoted field. Then use MINIMAL.
    protected QuoteMode guessQuoteModeByLines(String... lines) {
        List<Boolean> lineQuotationFlags = Lists.newArrayList();
        for (String line : lines) {
            List<String> columns = readCsvLine(line);
            List<QuoteState> columnQuotationFlags = getColumnQuotationFlags(columns);
            boolean allColumnsQuotedOrEmpty = columnQuotationFlags.stream().allMatch(f -> f == QuoteState.EMPTY || f == QuoteState.QUOTED);
            lineQuotationFlags.add(allColumnsQuotedOrEmpty);
        }
        boolean allQuotedOrEmpty = lineQuotationFlags.stream().allMatch(f -> f);
        if (allQuotedOrEmpty) {
            return QuoteMode.ALL;
        } else {
            return QuoteMode.MINIMAL;
        }
    }

    private List<QuoteState> getColumnQuotationFlags(List<String> columns) {
        return columns.stream()
                .map(column ->
                        StringUtils.isEmpty(column) ? QuoteState.EMPTY :
                                startsWith(column,quote) &&
                                endsWith(column, quote) ? QuoteState.QUOTED : QuoteState.UNQUOTED
                )
                .collect(Collectors.toList());
    }

    public List<String> readCsvLine(String line) {
        List<String> columns = Lists.newArrayList();
        line = line.trim();

        int unprocessedOffset = 0;
        String unprocessedLinePart = line;

        int nextDelimPos;
        do {
            if (startsWith(unprocessedLinePart, quote)) {
                int endingQuotePos = unprocessedLinePart.indexOf(quote, 1);
                if (endingQuotePos == -1) {
                    log.warn("Ignoring unmatched quote in " + unprocessedLinePart);
                    nextDelimPos = unprocessedLinePart.indexOf(delim);
                } else {
                    nextDelimPos = unprocessedLinePart.indexOf(delim, endingQuotePos);
                }
            } else {
                nextDelimPos = unprocessedLinePart.indexOf(delim);
            }
            int columnStart = unprocessedOffset;
            int columnEnd = nextDelimPos != -1 ? unprocessedOffset + nextDelimPos : line.length();
            columns.add(line.substring(columnStart, columnEnd));

            if (nextDelimPos == -1) {
                break;
            }
            unprocessedOffset = unprocessedOffset + nextDelimPos + 1;
            unprocessedLinePart = unprocessedLinePart.substring(nextDelimPos + 1);

        } while (true);

        return CsvUtil.fixNulls(columns);
    }

    private static boolean startsWith(String s, char c) {
        if (StringUtils.isEmpty(s)) {
            return false;
        }
        return s.charAt(0) == c;
    }

    private static boolean endsWith(String s, char c) {
        if (StringUtils.isEmpty(s)) {
            return false;
        }
        return s.charAt(s.length() - 1) == c;
    }

    private enum QuoteState {
        EMPTY, QUOTED, UNQUOTED;
    }
}
