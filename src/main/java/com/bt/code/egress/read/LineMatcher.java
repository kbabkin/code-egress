package com.bt.code.egress.read;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LineMatcher {
    private final GroupMatcher wordMatcher;
    private final ReportMatcher reportMatcher;

    public WordMatch nextMatch(LineToken prev, LineLocation lineLocation) {
        LineToken lineToken = prev;
        while ((lineToken = lineToken.nextToken()) != null) {
            String word = lineToken.getWordLowerCase();
            String matchReason = wordMatcher.getMatchReason(word);
            if (matchReason == null) {
                continue;
            }
            // reportMatcher can return null
            return new WordMatch(lineToken, matchReason, reportMatcher.getAllowed(lineToken, lineLocation));
        }
        return null;
    }

}
