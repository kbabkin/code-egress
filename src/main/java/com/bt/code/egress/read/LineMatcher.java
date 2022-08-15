package com.bt.code.egress.read;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LineMatcher {
    private final GroupMatcher wordMatcher;
    private final WordMatcher lineNegativeMatcher;

    public WordMatch nextMatch(LineToken prev) {
        LineToken lineToken = prev;
        while ((lineToken = lineToken.nextToken()) != null) {
            String word = lineToken.getWordLowerCase();
            String matchReason = wordMatcher.getMatchReason(word);
            if (matchReason == null) {
                continue;
            }
            if (lineNegativeMatcher != null) {
                String lineLower = lineToken.getLineLowerCase();
                if (lineNegativeMatcher.getMatchReason(lineLower) != null) {
                    //todo log ignore
                    return null;
                }
            }
            return new WordMatch(lineToken, matchReason);
        }
        return null;
    }

}
