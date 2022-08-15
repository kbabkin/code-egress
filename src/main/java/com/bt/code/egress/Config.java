package com.bt.code.egress;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


@Configuration
@EnableConfigurationProperties
@ConfigurationProperties
@Data
public class Config {
    MatchingGroups file = new MatchingGroups();
    MatchingGroups word = new MatchingGroups();
    MapGroup replace = new MapGroup();

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

}
