package com.bt.code.egress.write;

import lombok.Value;

import java.nio.file.Path;
import java.util.List;

@Value
public class FileCompleted {
    Path file;
    List<String> originalLines;
    List<String> replacedLines;

    @FunctionalInterface
    public interface Listener {
        void onFileCompleted(FileCompleted fileCompleted);
    }

    public boolean isChanged() {
        return !originalLines.equals(replacedLines);
    }
}
