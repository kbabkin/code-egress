package com.bt.code.egress.file;

import com.bt.code.egress.process.FileLocation;
import com.bt.code.egress.report.FileErrors;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;

@Slf4j
public class ReaderCharsetSelector {

    public static <T> T doWithBufferedReader(FileLocation file, BufferedReaderFunction<T> function,
                                             Charset... additionalCharsets) throws IOException {
        Charset charset = StandardCharsets.UTF_8;
        Charset newCharset;
        Iterator<Charset> additionalCharsetIterator = Arrays.asList(additionalCharsets).iterator();

        for (int i = 0; i <= additionalCharsets.length; i++) {
            try (BufferedReader bufferedReader = LocalFiles.newBufferedReader(file.getFilePath(), charset)) {
                return function.apply(bufferedReader);
            } catch (MalformedInputException e) {
                FileErrors.addError(file.toReportedPath(), String.format("%s encoding incompatible", charset));
                if (additionalCharsetIterator.hasNext()) {
                    newCharset = additionalCharsetIterator.next();
                    log.error("{} encoding incompatible, trying {}: {}", charset, newCharset, file);
                    charset = newCharset;
                } else {
                    throw e;
                }
            }
        }
        throw new IllegalArgumentException("All charsets exhausted while trying to create BufferedReader");
    }

    @FunctionalInterface
    public interface BufferedReaderFunction<T> {

        T apply(BufferedReader br) throws IOException;

    }
}
