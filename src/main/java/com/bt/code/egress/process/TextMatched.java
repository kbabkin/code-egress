package com.bt.code.egress.process;

import com.bt.code.egress.read.LineLocation;
import com.bt.code.egress.read.LineToken;
import lombok.Value;

@Value
public class TextMatched {
    LineLocation lineLocation;
    LineToken lineToken;
    Boolean allowed;
    String replacement;
    String comment;

    @FunctionalInterface
    public interface Listener {
        void onMatched(TextMatched textMatched);
    }

    @Override
    public String toString() {
        return "TextMatched{file=" + lineLocation.getFile() +
                "#" + lineLocation.getLineNum() +
                ":" + lineToken.getStartPos() +
                "-" + lineToken.getEndPos() +
                ", allowed=" + allowed +
                ", replacement='" + replacement + '\'' +
                ", comment='" + comment + '\'' +
                '}';
    }
}
