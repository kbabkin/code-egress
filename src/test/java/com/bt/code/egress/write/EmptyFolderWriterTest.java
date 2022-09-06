package com.bt.code.egress.write;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class EmptyFolderWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void testPrepareZip() throws IOException {
        FolderWriter folderWriter = new EmptyFolderWriter(Paths.get("."));
        Path sourceZip = tempDir.resolve(Paths.get("test-source.zip.tmp"));
        Files.write(sourceZip, Lists.newArrayList("new"));

        Path targetZip = tempDir.resolve(Paths.get("test-target.zip.tmp"));
        Files.write(targetZip, Lists.newArrayList("old"));
        assertTrue(Files.exists(targetZip));

        //We have old file and call prepareZip for the 1st time,
        // old content should be overwritten
        folderWriter.prepareZip(sourceZip, targetZip);
        assertTrue(Files.exists(targetZip));
        assertEquals(Lists.newArrayList("new"), Files.readAllLines(targetZip));

        //Make sure subsequent calls to prepareZip to not re-copy source
        Files.write(sourceZip, Lists.newArrayList("new-updated"));
        folderWriter.prepareZip(sourceZip, targetZip);
        assertEquals(Lists.newArrayList("new"), Files.readAllLines(targetZip));
    }
}