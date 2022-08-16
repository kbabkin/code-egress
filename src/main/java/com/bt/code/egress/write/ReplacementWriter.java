package com.bt.code.egress.write;

import java.util.List;

@FunctionalInterface
public interface ReplacementWriter {
    void write(String file, List<String> replacedLines);
}
