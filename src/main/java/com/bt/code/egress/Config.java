package com.bt.code.egress;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.*;


@Configuration
@EnableConfigurationProperties
@ConfigurationProperties
@Data
public class Config {
    MatchingGroups read = new MatchingGroups();
    MatchingGroups word = new MatchingGroups();
    MapGroup replace = new MapGroup();
    Allow allow = new Allow();
    CsvReplacementConfig csv = new CsvReplacementConfig();

    @Data
    public static class MatchingGroups {
        MatchingGroup guard = new MatchingGroup();
        MatchingGroup ignore = new MatchingGroup();
    }

    @Data
    public static class MatchingGroup {
        Set<String> values = new HashSet<>();
        Set<String> patterns = new HashSet<>();
        Set<String> valueFiles = new HashSet<>();
        Set<String> patternFiles = new HashSet<>();
    }

    @Data
    public static class MapGroup {
        Map<String, String> values = new HashMap<>();
        Set<String> valueFiles = new HashSet<>();
    }

    @Data
    public static class Allow {
        Set<String> reportFiles = new HashSet<>();
    }

    @Data
    public static class CsvReplacementConfig {
        Boolean enabled;
        List<CsvFileDescriptor> files;

        public boolean includes(String filename) {
            return files.stream().anyMatch(f -> f.getFilename().equals(filename));
        }
        public CsvFileDescriptor get(String filename) {
            return files.stream().filter(f -> f.getFilename().equals(filename)).findFirst().orElse(null);
        }
    }

    @Data
    public static class CsvFileDescriptor {
        String filename;
        Columns columns;
    }

    @Data
    public static class Columns {
        Map<String, String> replace = new LinkedHashMap<>();
        List<String> clear = new ArrayList<>();
    }
}
