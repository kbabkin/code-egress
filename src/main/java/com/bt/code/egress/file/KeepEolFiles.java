package com.bt.code.egress.file;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class KeepEolFiles {
    public static Stream<String> read(BufferedReader bufferedReader) {
        return StreamSupport.stream(new LastEolIterator(bufferedReader), false);
    }

    public static void write(BufferedWriter writer, List<String> lines) throws IOException {
        Objects.requireNonNull(lines);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            writer.append(line);
            if (i < lines.size() - 1) {
                writer.newLine();
            }
        }
    }

    @RequiredArgsConstructor
    static class LastEolIterator implements Spliterator<String> {
        private final BufferedReader bufferedReader;
        private boolean prevEOL;

        @SneakyThrows
        @Override
        public boolean tryAdvance(Consumer<? super String> action) {
            StringBuilder stringBuilder = new StringBuilder();
            int nextChar;
            while ((nextChar = bufferedReader.read()) != -1) {
                if (nextChar == '\n') {
                    prevEOL = true;
                    break;
                } else if (nextChar == '\r') {
                    bufferedReader.mark(1);
                    if (bufferedReader.read() != '\n') {
                        bufferedReader.reset();
                    }
                    prevEOL = true;
                    break;
                }
                prevEOL = false;
                stringBuilder.append((char) nextChar);
            }
            if (nextChar == -1 && stringBuilder.length() == 0) {
                if (!prevEOL) {
                    return false;
                }
                prevEOL = false;
            }
            action.accept(stringBuilder.toString());
            return true;
        }

        @Override
        public Spliterator<String> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return 0;
        }

        @Override
        public int characteristics() {
            return 0;
        }
    }
}
