package com.bt.code.egress.read;

import com.bt.code.egress.Config;
import lombok.RequiredArgsConstructor;
import org.springframework.util.AntPathMatcher;

import java.util.Set;

@RequiredArgsConstructor
public class FilePathMatcher {
    private final AntPathMatcher pathMatcher = new AntPathMatcher("/");
    private final Set<String> guard;
    private final Set<String> ignore;

    public static FilePathMatcher fromConfig(Config.MatchingSets matchingSets) {
        return new FilePathMatcher(matchingSets.getGuard().load().getValues(),
                matchingSets.getIgnore().load().getValues());
    }

    public boolean match(String name) {
        String slashed = name.replaceAll("\\\\", "/");
        return guard.stream().anyMatch(p -> pathMatcher.match(p, slashed))
                && ignore.stream().noneMatch(p -> pathMatcher.match(p, slashed));
    }
}
