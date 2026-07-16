package com.lutzseverino.minecraftsetup.domain;

import java.util.Optional;

public final class SetupCompliancePolicy {
  private final GateMode mode;
  private final FailurePolicy failurePolicy;

  public SetupCompliancePolicy(GateMode mode, FailurePolicy failurePolicy) {
    this.mode = mode;
    this.failurePolicy = failurePolicy;
  }

  public GateOutcome decide(
      boolean bypass, Optional<ComplianceRecord> record, ManifestFingerprint currentFingerprint) {
    if (mode == GateMode.OFF) {
      return GateOutcome.ALLOW_OFF;
    }
    if (bypass) {
      return GateOutcome.ALLOW_BYPASS;
    }
    if (record.filter(value -> value.matches(currentFingerprint)).isPresent()) {
      return GateOutcome.ALLOW_CURRENT;
    }
    if (mode == GateMode.ADVISORY) {
      return record.isPresent()
          ? GateOutcome.ALLOW_ADVISORY_OUTDATED
          : GateOutcome.ALLOW_ADVISORY_REQUIRED;
    }
    return record.isPresent() ? GateOutcome.DENY_SETUP_OUTDATED : GateOutcome.DENY_SETUP_REQUIRED;
  }

  public GateOutcome decideUnavailable(boolean bypass) {
    if (mode == GateMode.OFF) {
      return GateOutcome.ALLOW_OFF;
    }
    if (bypass) {
      return GateOutcome.ALLOW_BYPASS;
    }
    return failurePolicy == FailurePolicy.ALLOW
        ? GateOutcome.ALLOW_FAILURE
        : GateOutcome.DENY_FAILURE;
  }
}
