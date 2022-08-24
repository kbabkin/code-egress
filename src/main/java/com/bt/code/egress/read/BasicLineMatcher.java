package com.bt.code.egress.read;

import com.bt.code.egress.Config;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class BasicLineMatcher implements LineMatcher {
    private final Map<String, String> values;
    private final Map<Pattern, String> patterns;

    static BasicLineMatcher fromConfig(Config.MatchingMap matchingMap) {
        Config.MatchingMap.ValuesAndPatternsMap valuesAndPatterns = matchingMap.load();
        return new BasicLineMatcher(valuesAndPatterns.getValues(), valuesAndPatterns.getPatterns());
    }

    @Override
    public List<WordMatch> getMatches(String line) {
        String lineLowerCase = line.toLowerCase();
        List<WordMatch> result = Collections.emptyList();

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String value = entry.getKey();
            int pos = lineLowerCase.indexOf(value);
            while (pos >= 0) {
                result = addWordMatch(result,
                        new LineToken(line, pos, pos + value.length()),
                        "Value " + value, entry.getValue());
                pos = lineLowerCase.indexOf(value, pos + value.length());
            }
        }

        for (Map.Entry<Pattern, String> entry : patterns.entrySet()) {
            Pattern pattern = entry.getKey();
            Matcher matcher = pattern.matcher(lineLowerCase);
            while (matcher.find()) {
                result = addWordMatch(result,
                        new LineToken(line, matcher.start(), matcher.end()),
                        "Pattern " + pattern, entry.getValue());
            }
        }
        return result;
    }

    public static List<WordMatch> addWordMatch(List<WordMatch> result, LineToken lineToken, String matchReason, String template) {
        if (!lineToken.isWholeWord()) {
            return result;
        }
        if (result.isEmpty()) {
            result = new ArrayList<>();
        }
        result.add(new WordMatch(lineToken, matchReason, template, null));
        return result;

    }

}
