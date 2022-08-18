package com.bt.code.egress.process;

import com.bt.code.egress.read.LineLocation;
import lombok.RequiredArgsConstructor;
import com.bt.code.egress.write.FileCompleted;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class FileReplacer {
    private final LineReplacer lineReplacer;
    private final TextMatched.Listener textMatchedListener;

    public FileCompleted replace(Path file, BufferedReader bufferedReader) throws IOException {
        log.info("Read file: {}", file);
        String line;
        int lineNum = 0;
        ArrayList<String> originalLines = new ArrayList<>();
        List<String> replacedLines = new ArrayList<>();
        while (((line = bufferedReader.readLine()) != null)) {
            String replace = lineReplacer.replace(line, new LineLocation(file.toString(), ++lineNum), textMatchedListener);
            originalLines.add(line);
            replacedLines.add(replace);
        }
        return new FileCompleted(file, originalLines, replacedLines);
    }
}
