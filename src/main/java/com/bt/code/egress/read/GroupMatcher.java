package com.bt.code.egress.read;

import com.bt.code.egress.Config;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GroupMatcher {
    private final WordMatcher guardMatcher;
    private final WordMatcher ignoreMatcher;

    public static GroupMatcher fromConfigs(Config.MatchingGroups matchingGroups) {
        WordMatcher guard = WordMatcher.fromConfig(matchingGroups.getGuard());
        WordMatcher ignore = WordMatcher.fromConfig(matchingGroups.getIgnore());
        return new GroupMatcher(guard, ignore);
    }

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
