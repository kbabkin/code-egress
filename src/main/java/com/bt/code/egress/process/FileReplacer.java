package com.bt.code.egress.process;

import com.bt.code.egress.read.LineLocation;
import com.bt.code.egress.write.FileCompleted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class FileReplacer {
    private final LineReplacer lineReplacer;
    private final Matched.Listener matchedListener;

    public FileCompleted replace(String file, InputStream inputStream) throws IOException {
        log.info("Read file: {}", file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        int lineNum = 0;
        ArrayList<String> originalLines = new ArrayList<>();
        List<String> replacedLines = new ArrayList<>();
        while (((line = reader.readLine()) != null)) {
            String replace = lineReplacer.replace(line, new LineLocation(file, ++lineNum), matchedListener);
            originalLines.add(line);
            replacedLines.add(replace);
        }
        return new FileCompleted(file, originalLines, replacedLines);
    }
}
