package com.bt.code.egress.process;

import com.bt.code.egress.file.KeepEolFiles;
import com.bt.code.egress.read.LineLocation;
import com.bt.code.egress.write.FileCompleted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class TextFileReplacer implements FileReplacer {
    private final LineReplacer lineReplacer;
    private final ContextGenerator contextGenerator;

    @Override
    public FileCompleted replace(FileLocation file, BufferedReader bufferedReader) throws IOException {
        log.info("Process file as plain text: {}", file);

        int lineNum = 0;
        List<String> originalLines = new ArrayList<>();
        List<String> replacedLines = new ArrayList<>();
        for (Iterator<String> i = KeepEolFiles.read(bufferedReader).iterator(); i.hasNext(); ) {
            String line = i.next();
            String replace = lineReplacer.replace(line, new LineLocation(file.toReportedPath(), ++lineNum), contextGenerator);
            originalLines.add(line);
            replacedLines.add(replace);
        }
        return new FileCompleted(file, originalLines, replacedLines);
    }

}
