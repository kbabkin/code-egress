package com.bt.code.egress.util;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.Console;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
@Slf4j
public class ConfirmationUtil {
    private static final Map<String, Boolean> ALLOWED_ANSWERS = new HashMap<String, Boolean>() {
        {
            put("Y", true);
            put("y", true);
            put("N", false);
            put("n", false);
        }
    };

    @SneakyThrows
    public boolean confirm(String message) {
        Console console = System.console();
        if (console == null) {
            throw new RuntimeException("System.console() not available. This operation requires interactive mode and is aborted.");
        }
        console.printf("%s[Y/N]:");
        String answer;
        do {
            answer = console.readLine();
        } while (!ALLOWED_ANSWERS.containsKey(answer));

        return ALLOWED_ANSWERS.get(answer);
    }
}
