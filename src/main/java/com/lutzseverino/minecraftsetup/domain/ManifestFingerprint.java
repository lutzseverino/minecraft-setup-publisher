package com.lutzseverino.minecraftsetup.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public record ManifestFingerprint(String value) {
  private static final Pattern V1 = Pattern.compile("msm-v1-sha256:[0-9a-f]{64}");

  public ManifestFingerprint {
    Objects.requireNonNull(value, "value");
    if (!V1.matcher(value).matches()) {
      throw new IllegalArgumentException("Unsupported manifest fingerprint");
    }
  }
}
