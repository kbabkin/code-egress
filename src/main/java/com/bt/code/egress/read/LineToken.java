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

    public String getWordLowerCase() {
        if (wordLowerCase == null) {
            wordLowerCase = line.substring(startPos, endPos).toLowerCase();
        }
        return wordLowerCase;
    }

    public String getBefore(LineToken prevToken) {
        return line.substring(prevToken.getEndPos(), startPos);
    }

    public String getAfter() {
        return line.substring(endPos);
    }

    public LineToken nextToken() {
        int startPos = this.endPos;

        while (startPos < line.length() && !isAlphabetic(line.codePointAt(startPos))) {
            startPos++;
        }
        if (startPos >= line.length()) {
            return null;
        }

        int endPos = startPos + 1;
        while (endPos < line.length() && isAlphabetic(line.codePointAt(endPos))) {
            endPos++;
        }
        return new LineToken(line, startPos, endPos);
    }

    boolean isAlphabetic(int codePoint) {
//        return Character.isAlphabetic(codePoint);
        return (codePoint >= 'a' && codePoint <= 'z')
                || (codePoint >= 'A' && codePoint <= 'Z')
                || (codePoint >= '0' && codePoint <= '9')
                || codePoint == '_'
                || codePoint == '-'
                || codePoint == '~'
                ;
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
