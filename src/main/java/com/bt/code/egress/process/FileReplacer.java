package com.bt.code.egress.process;

import com.bt.code.egress.write.FileCompleted;

import java.io.BufferedReader;
import java.io.IOException;

@FunctionalInterface
public interface FileReplacer {
    FileCompleted replace(FileLocation file, BufferedReader bufferedReader) throws IOException;
}
