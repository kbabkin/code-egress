package com.bt.code.egress.process;

import com.bt.code.egress.read.*;
import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class LineReplacer {

    private final LineMatcher lineMatcher;
    private final WordReplacer wordReplacer;

    public String replace(String line, LineLocation lineLocation, Consumer<Matched> listener) {
        LineToken lineToken = new LineToken(line);
        WordMatch wordMatch;
        String processed = null;
        String unprocessed = line;
        while ((wordMatch = lineMatcher.nextMatch(lineToken)) != null) {
            lineToken = wordMatch.getLineToken();
            String replacement = wordReplacer.replace(lineToken.getWordLowerCase());
            processed = processed == null
                    ? lineToken.getBefore() + replacement
                    : processed + lineToken.getBefore() + replacement;
            unprocessed = lineToken.getAfter();
            listener.accept(new Matched(lineLocation, lineToken, replacement, wordMatch.getReason()));
        }
        return processed == null ? unprocessed : processed + unprocessed;
    }


}
