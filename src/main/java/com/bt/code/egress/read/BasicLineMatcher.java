package com.bt.code.egress.read;

import com.bt.code.egress.Config;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BasicLineMatcher implements LineMatcher {
    private final Map<String, Map<String, Map<String, Map<String, String>>>> valuesBy3Prefixes;
    private final Map<Pattern, String> patterns;

    public BasicLineMatcher(Map<String, String> values, Map<Pattern, String> patterns) {
        this.valuesBy3Prefixes = values.entrySet().stream()
                .collect(Collectors.groupingBy(e -> getPrefix(e.getKey(), 1),
                        Collectors.groupingBy(e -> getPrefix(e.getKey(), 2),
                                Collectors.groupingBy(e -> getPrefix(e.getKey(), 3),
                                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))));

        this.patterns = patterns;
    }

    static String getPrefix(String str, int len) {
        return str.length() > len ? str.substring(0, len) : str;
    }

    static BasicLineMatcher fromConfig(Config.MatchingMap matchingMap) {
        Config.MatchingMap.ValuesAndPatternsMap valuesAndPatterns = matchingMap.load();
        return new BasicLineMatcher(valuesAndPatterns.getValues(), valuesAndPatterns.getPatterns());
    }

    @Override
    public List<WordMatch> getMatches(String line) {
        String lineLowerCase = line.toLowerCase();
        List<WordMatch> result = Collections.emptyList();

        for (Map.Entry<String, Map<String, Map<String, Map<String, String>>>> by3Prefixes : valuesBy3Prefixes.entrySet()) {
            int posBy3 = lineLowerCase.indexOf(by3Prefixes.getKey());
            if (posBy3 < 0) {
                continue;
            }
            for (Map.Entry<String, Map<String, Map<String, String>>> by2Prefix1es : by3Prefixes.getValue().entrySet()) {
                int posBy2 = lineLowerCase.indexOf(by2Prefix1es.getKey(), posBy3);
                if (posBy2 < 0) {
                    continue;
                }
                for (Map.Entry<String, Map<String, String>> by1Prefix : by2Prefix1es.getValue().entrySet()) {
                    int posBy1 = lineLowerCase.indexOf(by1Prefix.getKey(), posBy2);
                    if (posBy1 < 0) {
                        continue;
                    }
                    for (Map.Entry<String, String> entry : by1Prefix.getValue().entrySet()) {
                        String value = entry.getKey();
                        int pos = lineLowerCase.indexOf(value, posBy1);
                        while (pos >= 0) {
                            result = addWordMatch(result,
                                    new LineToken(line, pos, pos + value.length()),
                                    "Value " + value, entry.getValue());
                            pos = lineLowerCase.indexOf(value, pos + value.length());
                        }
                    }
                }
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
        // template from value has priority over template from pattern
        if (result.stream()
                .filter(m -> lineToken.equals(m.getLineToken()))
                .anyMatch(m -> StringUtils.isNotBlank(m.getReplacement()) || StringUtils.isNotBlank(m.getTemplate()))) {
            return result;
        }
        if (result.isEmpty()) {
            result = new ArrayList<>();
        }
        result.add(new WordMatch(lineToken, matchReason, template, null));
        return result;

    }

}
