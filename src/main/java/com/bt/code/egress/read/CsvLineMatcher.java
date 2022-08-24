package com.bt.code.egress.read;

import com.bt.code.egress.Config;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.collections.CollectionUtils.*;

@Slf4j
public class CsvLineMatcher implements LineMatcher {

    private final Config.Columns csvColumnConfig;

    private final CsvParser csvParser;
    private final String filename;

    public CsvLineMatcher(Config.Columns csvColumnConfig,
                          String header, String delim, String quote,
                          String filename) {
        this.csvColumnConfig = csvColumnConfig;
        this.csvParser = new CsvParser(header, delim, quote);
        this.filename = filename;
    }


    @Override
    public List<WordMatch> getMatches(String line) {
        LineToken[] columns = csvParser.readCsvLine(line);
        List<WordMatch> replacements = replaceColumns(csvColumnConfig.getReplace(), columns, line);
        List<WordMatch> cleanups = clearColumns(csvColumnConfig.getClear(), columns, line);
        List<WordMatch> fills = fillColumns(csvColumnConfig.getFill(), columns, line);

        return merge(replacements, cleanups, fills, line);
    }


    private List<WordMatch> replaceColumns(Map<String, String> replaceConfig, LineToken[] columns, String line) {
        List<WordMatch> replacements = Lists.newArrayList();
        for (String targetColumn : replaceConfig.keySet()) {
            String sourceColumn = replaceConfig.get(targetColumn);
            Integer sourceColumnIndex = csvParser.getColumnIndex(sourceColumn);
            Integer targetColumnIndex = csvParser.getColumnIndex(targetColumn);

            if (sourceColumnIndex == null) {
                throw new RuntimeException(String.format("Source column %s not found in file %s",
                        sourceColumn, filename));
            }

            if (targetColumnIndex == null) {
                throw new RuntimeException(String.format("Target column %s not found in file %s",
                        targetColumn, filename));
            }

            replacements.addAll(
                    createReplacement(columns, sourceColumnIndex, targetColumnIndex, line,
                            String.format("%s->%s", sourceColumn, targetColumn)));
        }
        return replacements;
    }

    private List<WordMatch> clearColumns(List<String> clearConfig, LineToken[] columns, String line) {
        //Record custom csv cleanups
        List<WordMatch> cleanups = Lists.newArrayList();
        for (String clearedColumn : clearConfig) {
            Integer clearedColumnIndex = csvParser.getColumnIndex(clearedColumn);
            if (clearedColumnIndex == null) {
                log.error("Cleared column {} not found in file {}", clearedColumn, filename);
                continue;
            }
            cleanups.addAll(
                    createCleanup(columns, clearedColumnIndex, line,
                            clearedColumn + "->''"));
        }
        return cleanups;
    }

    private List<WordMatch> fillColumns(Map<String, String> fillConfig, LineToken[] columns, String line) {
        List<WordMatch> fills = Lists.newArrayList();
        for (String targetColumn : fillConfig.keySet()) {
            Integer targetColumnIndex = csvParser.getColumnIndex(targetColumn);
            String valueToFill = fillConfig.get(targetColumn);


            if (targetColumnIndex == null) {
                throw new RuntimeException(String.format("Target column %s not found in file %s",
                        targetColumn, filename));
            }

            fills.addAll(
                    createFill(columns, targetColumnIndex, line,
                            String.format("%s->'%s'", targetColumn, valueToFill), valueToFill));
        }
        return fills;
    }

    private List<WordMatch> createReplacement(LineToken[] columns,
                                              int sourceColumnIndex, int targetColumnIndex,
                                              String line, String reason) {
        List<WordMatch> replacement = Lists.newArrayList();
        if (sourceColumnIndex >= columns.length) {
            throw new RuntimeException(String.format("Out of bounds : Could not copy FROM column #%s in row : %s",
                    sourceColumnIndex, line));
        } else {
            LineToken sourceValue = columns[sourceColumnIndex];
            if (targetColumnIndex >= columns.length) {
                throw new RuntimeException(String.format("Out of bounds : Could not copy '%s' TO column #%s in row : %s",
                        sourceValue.getWord(), targetColumnIndex, line));
            } else {
                LineToken targetValue = columns[targetColumnIndex];
                replacement.add(new WordMatch(targetValue, reason, "", sourceValue.getWord()));
            }
        }
        return replacement;
    }

    private List<WordMatch> createCleanup(LineToken[] columns,
                                          int targetColumnIndex,
                                          String line, String reason) {
        List<WordMatch> cleanup = Lists.newArrayList();

        if (targetColumnIndex >= columns.length) {
            throw new RuntimeException(String.format("Out of bounds : Could not cleanup column #%s in row : %s",
                    targetColumnIndex, line));
        } else {
            LineToken targetValue = columns[targetColumnIndex];
            cleanup.add(new WordMatch(targetValue, reason, "", ""));
            //null for replacement would mean 'no replacement' but empty string means cleanup
        }
        return cleanup;
    }

    private List<WordMatch> createFill(LineToken[] columns,
                                       int targetColumnIndex,
                                       String line, String reason, String newValue) {
        List<WordMatch> fill = Lists.newArrayList();

        if (targetColumnIndex >= columns.length) {
            throw new RuntimeException(String.format("Out of bounds : Could not fill column #%s in row : %s",
                    targetColumnIndex, line));
        } else {
            LineToken targetValue = columns[targetColumnIndex];
            fill.add(new WordMatch(targetValue, reason, "", newValue));
        }
        return fill;
    }

    protected List<WordMatch> merge(List<WordMatch> replacements,
                                  List<WordMatch> cleanups,
                                  List<WordMatch> fills,
                                  String line) {
        Collection<WordMatch> allMatches = union(union(replacements, cleanups), fills);
        if (CollectionUtils.isEmpty(allMatches)) {
            return Collections.emptyList();
        }
        LinkedList<WordMatch> allMatchesSorted = new LinkedList<>(allMatches);
        allMatchesSorted.sort(Comparator.comparing(m -> m.getLineToken().getStartPos()));
        //ha ha who is sure where linked list's last element is ? -)
        int startPos = allMatchesSorted.getFirst().getLineToken().getStartPos();
        int endPos = allMatchesSorted.getLast().getLineToken().getEndPos();

        return Collections.singletonList(new WordMatch(
                new LineToken(line, startPos, endPos),
                allMatches.stream().map(WordMatch::getReason).collect(Collectors.joining(";")),
                "",
                applyReplacements(line, allMatchesSorted, startPos, endPos)
        ));

    }

    protected String applyReplacements(String line, Collection<WordMatch> sortedMatches, int startPos, int endPos) {
        int processedPos = startPos;
        String processedLine = null;

        for (WordMatch wm : sortedMatches) {
            String replacement = wm.getReplacement();
            String withBefore = line.substring(processedPos, wm.getLineToken().getStartPos()) + replacement;
            processedLine = processedLine == null ? withBefore : processedLine + withBefore;
            processedPos = wm.getLineToken().getEndPos();
            if (processedPos >= endPos) {
                break;
            }
        }

        return processedLine;
    }
}
