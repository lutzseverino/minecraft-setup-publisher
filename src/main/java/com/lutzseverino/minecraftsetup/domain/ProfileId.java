package com.lutzseverino.minecraftsetup.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public record ProfileId(String value) {
  private static final Pattern VALID = Pattern.compile("[A-Za-z0-9._-]{1,80}");

  public ProfileId {
    Objects.requireNonNull(value, "value");
    if (!VALID.matcher(value).matches()) {
      throw new IllegalArgumentException("Invalid setup profile ID");
    }
  }
}
