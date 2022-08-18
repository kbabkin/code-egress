package com.bt.code.egress.read;

import com.bt.code.egress.Config;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class BasicLineMatcher implements LineMatcher {
    private final Set<String> values;
    private final List<Pattern> patterns;

    static BasicLineMatcher fromConfig(Config.MatchingGroup matchingGroup) {
        Config.ValuesAndPatterns valuesAndPatterns = Config.ValuesAndPatterns.fromConfig(matchingGroup);
        return new BasicLineMatcher(valuesAndPatterns.getValues(), valuesAndPatterns.getPatterns());
    }

    @Override
    public List<WordMatch> getMatches(String line) {
        String lineLowerCase = line.toLowerCase();
        List<WordMatch> result = Collections.emptyList();

        for (String value : values) {
            int pos = lineLowerCase.indexOf(value);
            while (pos >= 0) {
                result = addWordMatch(result,
                        new LineToken(line, pos, pos + value.length()),
                        "Value " + value);
                pos = lineLowerCase.indexOf(value, pos + value.length());
            }
        }

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(lineLowerCase);
            while (matcher.find()) {
                result = addWordMatch(result,
                        new LineToken(line, matcher.start(), matcher.end()),
                        "Pattern " + pattern);
            }
        }
        return result;
    }

    public static List<WordMatch> addWordMatch(List<WordMatch> result, LineToken lineToken, String matchReason) {
        if (!lineToken.isWholeWord()) {
            return result;
        }
        if (result.isEmpty()) {
            result = new ArrayList<>();
        }
        result.add(new WordMatch(lineToken, matchReason));
        return result;

    }

}
