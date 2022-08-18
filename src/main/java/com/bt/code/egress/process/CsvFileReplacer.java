package com.bt.code.egress.process;

import com.bt.code.egress.Config;
import com.bt.code.egress.write.FileCompleted;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Slf4j
@RequiredArgsConstructor
public class CsvFileReplacer {

    private final Config.CsvReplacementConfig csvConfig;

    public boolean isEligibleForReplacement(String filename) {
        return csvConfig.includes(filename);
    }

    public boolean isEnabled() {
        return csvConfig.getEnabled();
    }

    public FileCompleted replace(Path path) throws IOException {
        List<String> originalLines = Files.readAllLines(path);
        String filename = path.getFileName().toString();

        Config.CsvFileDescriptor fileDescriptor = csvConfig.get(filename);
        if (fileDescriptor == null) {
            log.warn("Ineligible CSV file {}, skipping", filename);
            return new FileCompleted(path, originalLines, originalLines);
        }

        List<String[]> contents = Lists.newArrayList();
        try (BufferedReader bufferedReader = Files.newBufferedReader(path);
             CSVReader csvReader = new CSVReader(bufferedReader)) {
            contents = csvReader.readAll();
        }

        if (CollectionUtils.isEmpty(contents)) {
            log.warn("Empty CSV file {}, skipping", filename);
            return new FileCompleted(path, originalLines, originalLines);
        }

        String[] header = contents.get(0);
        Map<String, Integer> columnIndex = Maps.newHashMap();
        for (int i = 0; i < header.length; i++) {
            columnIndex.put(header[i], i);
        }

        //Perform custom csv replacements
        Map<String, String> replaceConfig = fileDescriptor.getColumns().getReplace();
        for (String targetColumn : replaceConfig.keySet()) {
            String sourceColumn = replaceConfig.get(targetColumn);
            Integer sourceColumnIndex = columnIndex.get(sourceColumn);
            Integer targetColumnIndex = columnIndex.get(targetColumn);

            if (sourceColumnIndex == null) {
                log.error("Source column {} not found in file {}", sourceColumn, filename);
                continue;
            }

            if (targetColumnIndex == null) {
                log.error("Target column {} not found in file {}", targetColumn, filename);
                continue;
            }

            copyColumnValue(contents, sourceColumnIndex, targetColumnIndex, rowIndex -> rowIndex > 0);
        }

        //Perform custom csv cleanups
        List<String> clearConfig = fileDescriptor.getColumns().getClear();
        for (String clearedColumn : clearConfig) {
            Integer clearedColumnIndex = columnIndex.get(clearedColumn);
            if (clearedColumnIndex == null) {
                log.error("Cleared column {} not found in file {}", clearedColumn, filename);
                continue;
            }
            clearColumnValue(contents, clearedColumnIndex, iRow -> iRow > 0);
        }

        try (StringWriter sw = new StringWriter();
             CSVWriter writer = new CSVWriter(sw)) {
            writer.writeAll(contents);
            return new FileCompleted(path, originalLines, Arrays.asList(sw.toString().split("\\n")));
        }
    }

    private void copyColumnValue(List<String[]> contents, int sourceColumnIndex, int targetColumnIndex, Predicate<Integer> rowFilter) {
        for (int iRow = 0; iRow < contents.size(); iRow++) {
            if (rowFilter.test(iRow)) {
                String[] row = contents.get(iRow);
                if (sourceColumnIndex >= row.length) {
                    log.warn("Out of bounds : Could not copy FROM column #{} in row #{}: {}", sourceColumnIndex, iRow, ArrayUtils.toString(row));
                } else {
                    String sourceValue = row[sourceColumnIndex];
                    if (targetColumnIndex >= row.length) {
                        log.warn("Out of bounds : Could not copy '{}' TO column #{} in row #{}: {}", sourceValue, targetColumnIndex, iRow, ArrayUtils.toString(row));
                    } else {
                        row[targetColumnIndex] = sourceValue;
                    }
                }
            }
        }
    }

    private void clearColumnValue(List<String[]> contents, int columnIndex, Predicate<Integer> rowFilter) {
        for (int iRow = 0; iRow < contents.size(); iRow++) {
            if (rowFilter.test(iRow)) {
                String[] row = contents.get(iRow);
                if (columnIndex >= row.length) {
                    log.warn("Out of bounds : Could not clear column #{} in row #{}: {}", columnIndex, iRow, ArrayUtils.toString(row));
                } else {
                    row[columnIndex] = "";
                }
            }
        }
    }
}