package com.bt.code.egress.read;

import java.util.ArrayList;
import java.util.List;

public interface LineMatcher {
    List<WordMatch> getMatches(String line);

    default LineMatcher and(LineMatcher other) {
        return line -> {
            List<WordMatch> matches1 = getMatches(line);
            List<WordMatch> matches2 = other.getMatches(line);
            if (matches1.isEmpty()) {
                return matches2;
            }
            if (matches2.isEmpty()) {
                return matches1;
            }
            List<WordMatch> result = new ArrayList<>(matches1);
            result.addAll(matches2);
            return result;
        };
    }
}
