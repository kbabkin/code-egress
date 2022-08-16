package com.bt.code.egress.process;

import com.bt.code.egress.read.LineLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Slf4j
public class FileReplacer {
    private final LineReplacer lineReplacer;
    private final Consumer<Matched> picker;

    public List<String> replace(String file, InputStream inputStream) throws IOException {
        log.info("Read file: {}", file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        int lineNum = 0;
        boolean changed = false;
        List<String> replacedLines = new ArrayList<>();
        while (((line = reader.readLine()) != null)) {
            String replace = lineReplacer.replace(line, new LineLocation(file, ++lineNum), picker);
            replacedLines.add(replace);
            if (line != replace && !line.equals(replace)) {
                changed = true;
            }
        }
        if (changed) {
            //todo store to file, can combine with picker
            log.debug("File {} is changed, new content:\n{}", file, String.join("\n", replacedLines));
            return replacedLines;
        } else {
            return null;
        }

    }
}
