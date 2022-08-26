package com.bt.code.egress.read;

import com.bt.code.egress.Config;
import lombok.RequiredArgsConstructor;
import org.springframework.util.AntPathMatcher;

import java.util.Set;

@RequiredArgsConstructor
public class FilePathMatcher {
    private static final AntPathMatcher pathMatcher = new AntPathMatcher("/");
    private final Set<String> guard;
    private final Set<String> ignore;

    public static FilePathMatcher fromConfig(Config.MatchingSets matchingSets) {
        return new FilePathMatcher(matchingSets.getGuard().load().getValues(),
                matchingSets.getIgnore().load().getValues());
    }

    public boolean match(String name) {
        String slashed = name.replaceAll("\\\\", "/");
        boolean xorFolder = !slashed.endsWith("/");
        return guard.stream().filter(p -> xorFolder ^ p.endsWith("/")).anyMatch(p -> pathMatcher.match(p, slashed))
                && ignore.stream().filter(p -> xorFolder ^ p.endsWith("/")).noneMatch(p -> pathMatcher.match(p, slashed));
    }

    public static boolean match(String pattern, String name) {
        String slashed = name.replaceAll("\\\\", "/");
        return pathMatcher.match(pattern, slashed);
    }
}
