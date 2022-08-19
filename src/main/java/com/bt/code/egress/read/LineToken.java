package com.bt.code.egress.read;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LineToken {
    @Getter
    private final String line;
    @Getter
    private final int startPos;
    @Getter
    private final int endPos;

    private String wordLowerCase;

    public LineToken(String line) {
        this(line, 0, 0);
    }

    public int getLength() {
        return endPos - startPos;
    }

    public String getWordLowerCase() {
        if (wordLowerCase == null) {
            wordLowerCase = line.substring(startPos, endPos).toLowerCase();
        }
        return wordLowerCase;
    }

    public boolean isWholeWord() {
        return (startPos <= 0 || !isAlphanumericAt(startPos - 1))
                && (endPos >= line.length() || !isAlphanumericAt(endPos));
    }

    public boolean isAlphanumericAt(int index) {
        int codePoint = line.codePointAt(index);
        return isAlphanumeric(codePoint);
    }

    public static boolean isAlphanumeric(int codePoint) {
        return Character.isAlphabetic(codePoint) || Character.isDigit(codePoint);
    }

    @Override
    public String toString() {
        return "LineToken{" +
                "wordLowerCase='" + getWordLowerCase() + '\'' +
                ", line='" + line + '\'' +
                ", startPos=" + startPos +
                ", endPos=" + endPos +
                '}';
    }
}
