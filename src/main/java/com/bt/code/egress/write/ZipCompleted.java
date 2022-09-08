package com.bt.code.egress.write;

import lombok.Value;

import java.nio.file.Path;

@Value
public class ZipCompleted {
    Path sourceZipAbsolutePath;
    Path sourceZipRelativePath;

    @FunctionalInterface
    public interface Listener {
        void onZipCompleted(ZipCompleted zipCompleted);
    }
}