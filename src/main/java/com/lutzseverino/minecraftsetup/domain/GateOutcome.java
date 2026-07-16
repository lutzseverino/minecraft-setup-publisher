package com.lutzseverino.minecraftsetup.domain;

public enum GateOutcome {
  ALLOW_OFF,
  ALLOW_BYPASS,
  ALLOW_CURRENT,
  ALLOW_ADVISORY_REQUIRED,
  ALLOW_ADVISORY_OUTDATED,
  ALLOW_FAILURE,
  DENY_SETUP_REQUIRED,
  DENY_SETUP_OUTDATED,
  DENY_FAILURE;

  public boolean denied() {
    return name().startsWith("DENY_");
  }

  public boolean needsSetup() {
    return this == ALLOW_ADVISORY_REQUIRED
        || this == ALLOW_ADVISORY_OUTDATED
        || this == DENY_SETUP_REQUIRED
        || this == DENY_SETUP_OUTDATED;
  }
}
