package com.lutzseverino.minecraftsetup.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record ChallengeCode(String value) {
    private static final Pattern VALID = Pattern.compile("[0-9A-HJKMNP-TV-Z]{16}");

    public ChallengeCode {
        Objects.requireNonNull(value, "value");
        value = value.replace("-", "").toUpperCase(Locale.ROOT);
        if (!VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid setup code");
        }
    }

    public String displayValue() {
        return value.substring(0, 4) + "-" + value.substring(4, 8) + "-"
                + value.substring(8, 12) + "-" + value.substring(12);
    }
}
