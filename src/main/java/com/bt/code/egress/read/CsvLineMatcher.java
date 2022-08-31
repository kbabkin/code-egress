package com.bt.code.egress.read;

import com.bt.code.egress.Config;
import com.bt.code.egress.report.Stats;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.collections.CollectionUtils.union;

@Slf4j
public class CsvLineMatcher implements LineMatcher {

    private final Config.Columns csvColumnConfig;

    private final CsvParser csvParser;
    private final String filename;
    @Getter
    private final Set<String> errors = new LinkedHashSet<>();

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
                addError(String.format("Source column '%s' not found in file %s",
                        sourceColumn, filename));
                continue;
            }

            if (targetColumnIndex == null) {
                addError(String.format("Target column '%s' not found in file %s",
                        targetColumn, filename));
                continue;
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
                addError(String.format("Cleared column '%s' not found in file %s", clearedColumn, filename));
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
                addError(String.format("Target column '%s' not found in file %s",
                        targetColumn, filename));
                continue;
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
            addError(String.format("Out of bounds : Could not copy FROM column #%s in row : %s",
                    sourceColumnIndex, line));
            return Collections.emptyList();
        } else {
            LineToken sourceValue = columns[sourceColumnIndex];
            if (targetColumnIndex >= columns.length) {
                addError(String.format("Out of bounds : Could not copy '%s' TO column #%s in row : %s",
                        sourceValue.getWord(), targetColumnIndex, line));
                return Collections.emptyList();
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
            addError(String.format("Out of bounds : Could not cleanup column #%s in row : %s",
                    targetColumnIndex, line));
            return Collections.emptyList();
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
            addError(String.format("Out of bounds : Could not fill column #%s in row : %s",
                    targetColumnIndex, line));
            return Collections.emptyList();
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
        LinkedList<WordMatch> allMatchesSortedByStartPos = new LinkedList<>(allMatches);
        allMatchesSortedByStartPos.sort(Comparator.comparing(m -> m.getLineToken().getStartPos()));

        //make sure the matches are consecutive and non-overlapping,
        //hence the sort order by startPos is the same as by endPos
        validateAndCleanup(allMatchesSortedByStartPos);

        int startPos = allMatchesSortedByStartPos.getFirst().getLineToken().getStartPos();
        int endPos = allMatchesSortedByStartPos.getLast().getLineToken().getEndPos();

        return Collections.singletonList(new WordMatch(
                new LineToken(line, startPos, endPos),
                allMatchesSortedByStartPos.stream().map(WordMatch::getReason).collect(Collectors.joining(";")),
                null,
                applyReplacements(line, allMatchesSortedByStartPos, startPos, endPos)
        ));

    }

    protected void validateAndCleanup(LinkedList<WordMatch> allMatchesSortedByStartPos) {
        int lastEndPos = 0;
        WordMatch current;
        for (Iterator<WordMatch> it = allMatchesSortedByStartPos.iterator(); it.hasNext(); ) {
            current = it.next();
            if (current.getLineToken().getStartPos() < lastEndPos) {
                it.remove();
            } else {
                lastEndPos = current.getLineToken().getEndPos();
            }
        }
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

    private void addError(String error) {
        Stats.addError(filename, error);
    }
}
