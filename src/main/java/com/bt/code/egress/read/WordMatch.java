package com.bt.code.egress.read;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode
public class WordMatch {
    LineToken lineToken;
    String reason;
    String template;

    String replacement;

    public boolean isCompatible(WordMatch other) {
        return this.lineToken.getEndPos() <= other.lineToken.getStartPos()
                || this.lineToken.getStartPos() >= other.lineToken.getEndPos();
    }

}
