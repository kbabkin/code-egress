package com.bt.code.egress.read;

import com.bt.code.egress.Config;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Find word in configured set.
 * Used to match line tokenized into whole words, or to match ignored words.
 * Optimization: lookup in hash map whole words from tokenized line.
 */
@RequiredArgsConstructor
public class BasicWordMatcher implements WordMatcher {
    private final Map<String, String> values;
    private final Map<Pattern, String> patterns;
    private final Function<Matcher, Boolean> patternMatcher;

    public BasicWordMatcher(Map<String, String> values, Map<Pattern, String> patterns) {
        this(values, patterns, Matcher::matches);
    }

    public BasicWordMatcher patternPartOfWord() {
        return new BasicWordMatcher(values, patterns, Matcher::find);
    }

    public static BasicWordMatcher fromConfig(Config.MatchingSet matchingSet) {
        Config.MatchingSet.ValuesAndPatternsSet valuesAndPatterns = matchingSet.load();
        return fromConfig(valuesAndPatterns);
    }

    public static BasicWordMatcher fromConfig(Config.MatchingSet.ValuesAndPatternsSet valuesAndPatterns) {
        return new BasicWordMatcher(valuesAndPatterns.getValues().stream().collect(Collectors.toMap(Function.identity(), e -> "")),
                valuesAndPatterns.getPatterns().stream().collect(Collectors.toMap(Function.identity(), e -> "")));
    }

    public static BasicWordMatcher fromConfig(Config.MatchingMap matchingMap) {
        Config.MatchingMap.ValuesAndPatternsMap valuesAndPatterns = matchingMap.load();
        return new BasicWordMatcher(valuesAndPatterns.getValues(), valuesAndPatterns.getPatterns());
    }

    @Override
    public WordMatch getWordMatch(String word) {
        if (values.containsKey(word)) {
            return new WordMatch(new LineToken(word, 0, word.length()),
                    "Value " + word, values.get(word), null);
        }
        for (Map.Entry<Pattern, String> patternEntry : patterns.entrySet()) {
            Pattern pattern = patternEntry.getKey();
            Matcher matcher = pattern.matcher(word);
            if (patternMatcher.apply(matcher)) {
                return new WordMatch(new LineToken(word, matcher.start(), matcher.end()),
                        "Pattern " + pattern, patternEntry.getValue(), null);
            }
        }
        return null;
    }
}
