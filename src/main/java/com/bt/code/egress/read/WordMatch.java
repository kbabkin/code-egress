package com.bt.code.egress.read;

import lombok.Value;

@Value
public class WordMatch {
    LineToken lineToken;
    String reason;
    String template;

    public boolean isCompatible(WordMatch other) {
        return this.lineToken.getEndPos() <= other.lineToken.getStartPos()
                || this.lineToken.getStartPos() >= other.lineToken.getEndPos();
    }

}
