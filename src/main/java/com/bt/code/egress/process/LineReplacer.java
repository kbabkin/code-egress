package com.bt.code.egress.process;

import com.bt.code.egress.read.LineLocation;
import com.bt.code.egress.read.LineMatcher;
import com.bt.code.egress.read.LineToken;
import com.bt.code.egress.read.WordMatch;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LineReplacer {

    private final LineMatcher lineMatcher;
    private final WordReplacer wordReplacer;

    public String replace(String line, LineLocation lineLocation, TextMatched.Listener textMatchedListener) {
        LineToken lineToken = new LineToken(line);
        LineToken prevToken;
        WordMatch wordMatch;
        String processed = null;
        String unprocessed = line;
        while ((wordMatch = lineMatcher.nextMatch(lineToken, lineLocation)) != null) {
            prevToken = lineToken;
            lineToken = wordMatch.getLineToken();

            String replacement = wordReplacer.replace(lineToken.getWordLowerCase());
            String withBefore = (Boolean.TRUE.equals(wordMatch.getAllowed()))
                    ? lineToken.getLine().substring(prevToken.getEndPos(), lineToken.getEndPos())
                    : lineToken.getBefore(prevToken) + replacement;

            processed = processed == null ? withBefore : processed + withBefore;
            unprocessed = lineToken.getAfter();
            textMatchedListener.onMatched(new TextMatched(lineLocation, lineToken, wordMatch.getAllowed(), replacement, wordMatch.getReason()));
        }
        return processed == null ? unprocessed : processed + unprocessed;
    }


}
