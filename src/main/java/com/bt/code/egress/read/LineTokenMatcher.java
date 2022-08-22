package com.bt.code.egress.read;

import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Used as optimization - split line into words and lookup in hash map instead of searching each word in the line.
 */
@RequiredArgsConstructor
public class LineTokenMatcher implements LineMatcher {
    private final WordMatcher wordMatcher;

    @Override
    public List<WordMatch> getMatches(String line) {
        List<WordMatch> result = Collections.emptyList();
        LineToken lineToken = new LineToken(line);
        while ((lineToken = nextToken(lineToken)) != null) {
            String word = lineToken.getWordLowerCase();
            WordMatch wordMatch = wordMatcher.getWordMatch(word);
            if (wordMatch == null) {
                continue;
            }

            result = BasicLineMatcher.addWordMatch(result, lineToken, wordMatch.getReason(), wordMatch.getTemplate());
        }
        return result;
    }

    static LineToken nextToken(LineToken lineToken) {
        String line = lineToken.getLine();
        int startPos = lineToken.getEndPos();

        while (startPos < line.length() && !lineToken.isAlphanumericAt(startPos)) {
            startPos++;
        }
        if (startPos >= line.length()) {
            return null;
        }

        int endPos = startPos + 1;
        while (endPos < line.length() && lineToken.isAlphanumericAt(endPos)) {
            endPos++;
        }
        return new LineToken(line, startPos, endPos);
    }
}
