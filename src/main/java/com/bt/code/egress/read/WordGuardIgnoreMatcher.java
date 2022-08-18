package com.bt.code.egress.read;

import com.bt.code.egress.Config;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WordGuardIgnoreMatcher implements WordMatcher {
    private final WordMatcher guardMatcher;
    private final WordMatcher ignoreMatcher;

    public static WordGuardIgnoreMatcher fromConfigs(Config.MatchingGroups matchingGroups) {
        WordMatcher guard = BasicWordMatcher.fromConfig(matchingGroups.getGuard());
        WordMatcher ignore = BasicWordMatcher.fromConfig(matchingGroups.getIgnore());
        return new WordGuardIgnoreMatcher(guard, ignore);
    }

    @Override
    public String getMatchReason(String word) {
        String matchReason = guardMatcher.getMatchReason(word);
        if (matchReason != null) {
            String ignoreReason = ignoreMatcher.getMatchReason(word);
            if (ignoreReason == null) {
                return matchReason;
            } else {
//                        todo log or notify
            }
        }
        return null;
    }
}
