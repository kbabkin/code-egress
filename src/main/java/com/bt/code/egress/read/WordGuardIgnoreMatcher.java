package com.bt.code.egress.read;

import com.bt.code.egress.Config;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WordGuardIgnoreMatcher implements WordMatcher {
    private final WordMatcher guardMatcher;
    private final WordMatcher ignoreMatcher;

    public static WordGuardIgnoreMatcher fromConfigs(Config.MatchingSets matchingSets) {
        WordMatcher guard = BasicWordMatcher.fromConfig(matchingSets.getGuard());
        WordMatcher ignore = BasicWordMatcher.fromConfig(matchingSets.getIgnore()).patternPartOfWord();
        return new WordGuardIgnoreMatcher(guard, ignore);
    }

    @Override
    public WordMatch getWordMatch(String word) {
        WordMatch wordMatch = guardMatcher.getWordMatch(word);
        if (wordMatch != null) {
            WordMatch ignoreReason = ignoreMatcher.getWordMatch(word);
            if (ignoreReason == null) {
                return wordMatch;
            } else {
//                        todo log or notify
            }
        }
        return null;
    }
}
