package com.bt.code.egress.file;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class KeepEolFilesTest {

    @Test
    void read() {
        assertThat(getReadLines("win\r\nlinux\nmac\rmixed empty\r\n\rno EOL"))
                .isEqualTo(ImmutableList.of("win", "linux", "mac", "mixed empty", "", "no EOL"));
        assertThat(getReadLines("win\r\nlinux\nmac\rmixed empty\r\n\rwith EOL\n"))
                .isEqualTo(ImmutableList.of("win", "linux", "mac", "mixed empty", "", "with EOL", ""));
    }

    @Test
    void write() throws IOException {
        String s = System.lineSeparator();
        assertThat(getWriteContent(ImmutableList.of())).isEqualTo("");
        assertThat(getWriteContent(ImmutableList.of(""))).isEqualTo("");
        assertThat(getWriteContent(ImmutableList.of("", ""))).isEqualTo(s);
        assertThat(getWriteContent(ImmutableList.of("abc"))).isEqualTo("abc");
        assertThat(getWriteContent(ImmutableList.of("abc", "def"))).isEqualTo("abc" + s + "def");
        assertThat(getWriteContent(ImmutableList.of("abc", "def", ""))).isEqualTo("abc" + s + "def" + s);
    }

    @Test
    void cycle() throws IOException {
        String s = System.lineSeparator();
        checkCycle("abc" + s + "def");
        checkCycle("abc" + s + "def" + s);
        checkCycle(s);
        checkCycle("");
    }

    List<String> getReadLines(String content) {
        StringReader reader = new StringReader(content);
        BufferedReader bufferedReader = new BufferedReader(reader);
        return KeepEolFiles.read(bufferedReader).collect(Collectors.toList());
    }

    String getWriteContent(List<String> lines) throws IOException {
        StringWriter stringWriter = new StringWriter();
        BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
        KeepEolFiles.write(bufferedWriter, lines);
        bufferedWriter.close();
        return stringWriter.toString();
    }

    void checkCycle(String content) throws IOException {
        List<String> lines = getReadLines(content);
        assertThat(getWriteContent(lines)).isEqualTo(content);
    }
}
