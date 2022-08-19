package com.bt.code.egress.read;

import com.bt.code.egress.Config;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class BasicWordMatcher implements WordMatcher {
    private final Set<String> values;
    private final List<Pattern> patterns;

    public static BasicWordMatcher fromConfig(Config.MatchingGroup matchingGroup) {
        Config.ValuesAndPatterns valuesAndPatterns = Config.ValuesAndPatterns.fromConfig(matchingGroup);
        return new BasicWordMatcher(valuesAndPatterns.getValues(), valuesAndPatterns.getPatterns());
    }

    @Override
    public String getMatchReason(String word) {
        if (values.contains(word)) {
            return "Exact match: " + word;
        }
        for (Pattern pattern : patterns) {
            if (pattern.matcher(word).matches()) {
                return "Pattern match: " + pattern;
            }
        }
        return null;
    }
}
