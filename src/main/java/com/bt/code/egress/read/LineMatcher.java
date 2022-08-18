package com.bt.code.egress.read;

import java.util.List;

public interface LineMatcher {
    List<WordMatch> getMatches(String line);
}
