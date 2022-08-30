package com.bt.code.egress.generator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
public class PerformanceTestingDataGenerator {

    public static String[] STANDARD_CSV_NAMES = {"LegalEntity", "EM_User", "Institution", "Book"};

    public static void main(String[] args) throws IOException {
        int N_FOLDERS = 1;
        int ZIPS_PER_FOLDER = 10;
        int FILES_PER_ZIP = 30;
        int ADDITIONAL_LINES_PER_FILE = 200;

        int TXT_FILES_PER_FOLDER = 700;
        int CSV_FILES_PER_FOLDER = 300;

        //Adopt for your own paths
        String targetRoot = "/Work/projects/code-egress/testdata/";
        String fileSourcesDir = "C:/Work/projects/code-egress/src/test/resources/testdata";

        StopWatch timer = new StopWatch();
        timer.start();

        for (int iFolder = 0; iFolder < N_FOLDERS; iFolder++) {
            log.info("Generating folder {} of {}", iFolder, N_FOLDERS);

            String folderName = String.format("folder%03d", iFolder);
            Path folderPath = Paths.get(targetRoot, folderName);
            Files.createDirectories(folderPath);

            for (int iZip = 0; iZip < ZIPS_PER_FOLDER; iZip++) {
                String zipName = String.format("zip%03d.zip", iZip);
                Path zipPath = Paths.get(folderPath.toString(), zipName);

                for (int iFileInsideZip = 0; iFileInsideZip < FILES_PER_ZIP; iFileInsideZip++) {
                    String base = STANDARD_CSV_NAMES[(int) (Math.random() * 1000) % STANDARD_CSV_NAMES.length];
                    String filename = base + "_" + new Random().nextInt(100000) + ".csv";
                    Path sourcePath = Paths.get(fileSourcesDir, base + ".csv");

                    List<String> lines = generateLines(sourcePath, ADDITIONAL_LINES_PER_FILE);
                    addFileToZip(filename, zipPath, lines);
                }
            }

            for (int iFile = 0; iFile < TXT_FILES_PER_FOLDER; iFile++) {
                generateFile(fileSourcesDir, folderPath, "txt", ADDITIONAL_LINES_PER_FILE);
            }

            for (int iFile = 0; iFile < CSV_FILES_PER_FOLDER; iFile++) {
                generateFile(fileSourcesDir, folderPath, "csv", ADDITIONAL_LINES_PER_FILE);
            }
        }
        timer.stop();
        log.info("Generation completed in {}", timer);
    }

    private static List<String> generateLines(Path sourcePath, int count) throws IOException {
        List<String> lines = Files.readAllLines(sourcePath);

        for (int iAdditionalLine = 0; iAdditionalLine < count; iAdditionalLine++) {
            lines.add(lines.get(lines.size() - 1));
        }
        return lines;
    }

    private static void generateFile(String fileSourcesDir, Path folderPath, String extension, int count) throws IOException {

        String base = STANDARD_CSV_NAMES[(int) (Math.random() * 1000) % STANDARD_CSV_NAMES.length];
        Path sourcePath = Paths.get(fileSourcesDir, base + ".csv");
        List<String> lines = generateLines(sourcePath, count);

        Path newFilePath;
        String filename;
        do {
            filename = base + "_" + new Random().nextInt(100000) + "." + extension;
            newFilePath = Paths.get(folderPath.toString(), filename);
        } while (Files.exists(newFilePath));

        Files.write(Paths.get(folderPath.toString(), filename), lines);
    }

    private static void addFileToZip(String filename, Path zipPath, List<String> lines) throws IOException {
        Map<String, String> env = new HashMap<>();
        // Create the zip file if it doesn't exist
        env.put("create", "true");

        URI uri = URI.create("jar:file:" + zipPath.toString().replaceAll("\\\\", "/"));

        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
            Path pathInZipfile = zipfs.getPath(filename);
            Files.write(pathInZipfile, lines);
        }
    }
}
