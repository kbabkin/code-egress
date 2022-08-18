package com.bt.code.egress.read;

import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
//todo use as optimization
public class LineTokenMatcher implements LineMatcher {
    private final WordMatcher wordMatcher;

    @Override
    public List<WordMatch> getMatches(String line) {
        List<WordMatch> result = Collections.emptyList();
        LineToken lineToken = new LineToken(line);
        while ((lineToken = nextToken(lineToken)) != null) {
            String word = lineToken.getWordLowerCase();
            String matchReason = wordMatcher.getMatchReason(word);
            if (matchReason == null) {
                continue;
            }

            result = BasicLineMatcher.addWordMatch(result, lineToken, matchReason);
        }
        return result;
    }

    static LineToken nextToken(LineToken lineToken) {
        String line = lineToken.getLine();
        int startPos = lineToken.getEndPos();

        while (startPos < line.length() && !lineToken.isAlphabetic(startPos)) {
            startPos++;
        }
        if (startPos >= line.length()) {
            return null;
        }

        int endPos = startPos + 1;
        while (endPos < line.length() && lineToken.isAlphabetic(endPos)) {
            endPos++;
        }
        return new LineToken(line, startPos, endPos);
    }
}
