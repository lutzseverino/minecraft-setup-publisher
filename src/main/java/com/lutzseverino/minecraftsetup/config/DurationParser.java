package com.lutzseverino.minecraftsetup.config;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {
    private static final Pattern VALUE = Pattern.compile("([1-9][0-9]*)([smh])");

    private DurationParser() {
    }

    public static Duration parse(String input) {
        Matcher matcher = VALUE.matcher(input.strip().toLowerCase(Locale.ROOT));
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Use a duration such as 30s, 10m, or 1h");
        }
        long amount = Long.parseLong(matcher.group(1));
        return switch (matcher.group(2)) {
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            default -> throw new IllegalStateException("Unexpected duration unit");
        };
    }
}
