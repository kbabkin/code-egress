package com.bt.code.egress.process;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class BufferedReaderUtilTest {
     @Test
     public void testDoWithBufferedReader() throws IOException {
          byte[] UTF_8_bytes = new byte[]{0x74, (byte)0xC3, (byte)0xAD, 0x6D, 0x69, 0x64, 0x6F};
          byte[] ISO_8859_bytes = new byte[]{0x74, (byte)0xED, 0x6D, 0x69, 0x64, 0x6F};

          Path utf8TempFile = Files.createTempFile("utf8", "-test");
          Files.write(utf8TempFile, UTF_8_bytes);

          Path iso8859TempFile = Files.createTempFile("iso8859", "-test");
          Files.write(iso8859TempFile, ISO_8859_bytes);

          String fromUTF = BufferedReaderUtil.doWithBufferedReader(FileLocation.forFile(utf8TempFile), BufferedReader::readLine,
          StandardCharsets.ISO_8859_1);

          String fromISO8859 = BufferedReaderUtil.doWithBufferedReader(FileLocation.forFile(iso8859TempFile), BufferedReader::readLine,
                  StandardCharsets.ISO_8859_1);

          log.info("From utf: {}", fromUTF);
          log.info("From ISO: {}", fromISO8859);

          assertTrue(fromUTF.startsWith("t"));
          assertTrue(fromUTF.endsWith("mido"));

          assertTrue(fromISO8859.startsWith("t"));
          assertTrue(fromISO8859.endsWith("mido"));
     }
}