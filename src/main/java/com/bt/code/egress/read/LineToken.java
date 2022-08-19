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
        return (startPos <= 0 || !isAlphabetic(startPos - 1))
                && (endPos >= line.length() || !isAlphabetic(endPos));
    }

    public boolean isAlphabetic(int index) {
        int codePoint = line.codePointAt(index);
        return isAlphabeticChar(codePoint);
    }

    public static boolean isAlphabeticChar(int codePoint) {
//        return Character.isAlphabetic(codePoint);
        return (codePoint >= 'a' && codePoint <= 'z')
                || (codePoint >= 'A' && codePoint <= 'Z')
                || (codePoint >= '0' && codePoint <= '9')
                || codePoint == '_'
                || codePoint == '-'
                || codePoint == '~';
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
