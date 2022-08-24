package com.bt.code.egress.read;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class CsvParser {
    private final String delim;
    private final String quote;
    private final Map<String, Integer> columnIndex;

    public CsvParser(String header, String delim, String quote) {
        this.delim = delim;
        this.quote = quote;
        this.columnIndex = createIndex(header);
    }

    public String[] readCsvLineAsStrings(String line) {
        return Stream.of(readCsvLine(line)).map(LineToken::getWord).toArray(String[]::new);
    }

    public LineToken[] readCsvLine(String line) {
        List<LineToken> columns = Lists.newArrayList();

        int unprocessedOffset = 0;
        String unprocessedLinePart = line;

        int nextDelimPos;
        do {
            if (unprocessedLinePart.startsWith(quote)) {
                int endingQuotePos = unprocessedLinePart.indexOf(quote, quote.length());
                if (endingQuotePos == -1) {
                    log.warn("Ignoring unmatched quote in " + unprocessedLinePart);
                    nextDelimPos = unprocessedLinePart.indexOf(delim);
                } else {
                    nextDelimPos = unprocessedLinePart.indexOf(delim, endingQuotePos);
                }
            } else {
                nextDelimPos = unprocessedLinePart.indexOf(delim);
            }
            LineToken column = new LineToken(line, unprocessedOffset,
                    nextDelimPos != -1 ? unprocessedOffset + nextDelimPos : line.length());
            columns.add(column);

            if (nextDelimPos == -1)
                break;
            unprocessedOffset = unprocessedOffset + nextDelimPos + delim.length();
            unprocessedLinePart = unprocessedLinePart.substring(nextDelimPos + delim.length());

        } while (true);
        return columns.toArray(new LineToken[columns.size()]);
    }

    @Nullable
    public Integer getColumnIndex(String columnName) {
        return columnIndex.get(columnName);
    }

    private Map<String, Integer> createIndex(String firstLine) {
        String[] header = readCsvLineAsStrings(firstLine);
        Map<String, Integer> columnIndex = Maps.newHashMap();
        for (int i = 0; i < header.length; i++) {
            columnIndex.put(header[i], i);
        }
        return columnIndex;
    }
}
