package com.bt.code.egress.read;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LineToken {
    private final String line;
    private final int startPos;
    private final int endPos;

    private String wordLowerCase;
    private String lineLowerCase;

    public LineToken(String line) {
        this(line, 0, 0);
    }

    public String getWordLowerCase() {
        if (wordLowerCase == null) {
            wordLowerCase = line.substring(startPos, endPos).toLowerCase();
        }
        return wordLowerCase;
    }

    public String getLineLowerCase() {
        if (lineLowerCase == null) {
            lineLowerCase = line.toLowerCase();
        }
        return lineLowerCase;
    }

    public String getBefore() {
        return line.substring(0, startPos);
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
