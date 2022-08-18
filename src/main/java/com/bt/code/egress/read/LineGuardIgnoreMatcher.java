package com.bt.code.egress.read;

import com.bt.code.egress.Config;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class LineGuardIgnoreMatcher implements LineMatcher {

    private final LineMatcher guardMatcher;
    private final WordMatcher ignoreMatcher;

    public static LineGuardIgnoreMatcher fromConfigs(Config.MatchingGroups matchingGroups) {
        LineMatcher guard = BasicLineMatcher.fromConfig(matchingGroups.getGuard());
        WordMatcher ignore = BasicWordMatcher.fromConfig(matchingGroups.getIgnore());
        return new LineGuardIgnoreMatcher(guard, ignore);
    }

    public static LineGuardIgnoreMatcher fromConfigsOptimized(Config.MatchingGroups matchingGroups) {
        Config.ValuesAndPatterns guardVnP = Config.ValuesAndPatterns.fromConfig(matchingGroups.getGuard());
        Map<Boolean, List<String>> byAlphabetic = guardVnP.getValues().stream()
                .collect(Collectors.partitioningBy(w -> {
                    for (int i = 0; i < w.length(); i++) {
                        if (!LineToken.isAlphabeticChar(w.codePointAt(i))) {
                            return false;
                        }
                    }
                    return true;
                }));
        LineMatcher guard = new BasicLineMatcher(new HashSet<>(byAlphabetic.get(false)), guardVnP.getPatterns())
                .and(new LineTokenMatcher(new BasicWordMatcher(new HashSet<>(byAlphabetic.get(true)), Collections.emptyList())));
        WordMatcher ignore = BasicWordMatcher.fromConfig(matchingGroups.getIgnore());
        return new LineGuardIgnoreMatcher(guard, ignore);
    }

    @Override
    public List<WordMatch> getMatches(String line) {
        List<WordMatch> matches = guardMatcher.getMatches(line);
        if (matches.isEmpty()) {
            return matches;
        }
        return matches.stream()
                .filter(match -> {
                    String ignoreReason = ignoreMatcher.getMatchReason(match.getLineToken().getWordLowerCase());
                    if (ignoreReason == null) {
                        return true;
                    } else {
//                        todo log or notify
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

}
