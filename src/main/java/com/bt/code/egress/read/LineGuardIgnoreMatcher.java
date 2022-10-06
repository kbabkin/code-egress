package com.bt.code.egress.read;

import com.bt.code.egress.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class LineGuardIgnoreMatcher implements LineMatcher {

    private final LineMatcher guardMatcher;
    private final WordMatcher ignoreMatcher;

    public static LineGuardIgnoreMatcher fromConfigsRaw(Config.MatchingMaps matchingMaps) {
        LineMatcher guard = BasicLineMatcher.fromConfig(matchingMaps.getGuard());
        WordMatcher ignore = BasicWordMatcher.fromConfig(matchingMaps.getIgnore()).patternPartOfWord();
        return new LineGuardIgnoreMatcher(guard, ignore);
    }

    public static LineGuardIgnoreMatcher fromConfigs(Config.MatchingMaps matchingMaps) {
        Config.MatchingMap.ValuesAndPatternsMap guardVnP = matchingMaps.getGuard().load();
        Config.MatchingSet.ValuesAndPatternsSet ignoreVnP = matchingMaps.getIgnore().load();
        return fromConfigs(guardVnP, ignoreVnP);
    }

    public static LineGuardIgnoreMatcher fromConfigs(Config.MatchingMaps matchingMaps, Map<String, String> instructionReplacements) {
        Config.MatchingMap.ValuesAndPatternsMap guardVnP = matchingMaps.getGuard().load();
        Config.MatchingSet.ValuesAndPatternsSet ignoreVnP = matchingMaps.getIgnore().load();
        Map<String, String> guardValues = new HashMap<>(guardVnP.getValues());
        instructionReplacements.forEach((word, replacement) -> guardValues.computeIfAbsent(word, n -> replacement));
        return fromConfigs(new Config.MatchingMap.ValuesAndPatternsMap(guardValues, guardVnP.getPatterns()), ignoreVnP);
    }

    private static LineGuardIgnoreMatcher fromConfigs(Config.MatchingMap.ValuesAndPatternsMap guardVnP,
                                                      Config.MatchingSet.ValuesAndPatternsSet ignoreVnP) {
        Set<String> wholeWords = guardVnP.getValues().keySet().stream()
                .filter(w -> {
                    for (int i = 0; i < w.length(); i++) {
                        if (!LineToken.isAlphanumeric(w.codePointAt(i))) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toSet());
        HashMap<String, String> words = new HashMap<>(guardVnP.getValues());
        words.keySet().retainAll(wholeWords);
        HashMap<String, String> phrases = new HashMap<>(guardVnP.getValues());
        phrases.keySet().removeAll(wholeWords);
        log.info("Config: {} whole words, {} phrases", words.size(), phrases.size());

        LineMatcher guard = new BasicLineMatcher(phrases, guardVnP.getPatterns())
                .and(new LineTokenMatcher(new BasicWordMatcher(words, Collections.emptyMap())));
        WordMatcher ignore = BasicWordMatcher.fromConfig(ignoreVnP).patternPartOfWord();
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
                    WordMatch ignoreReason = ignoreMatcher.getWordMatch(match.getLineToken().getWordLowerCase());
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
