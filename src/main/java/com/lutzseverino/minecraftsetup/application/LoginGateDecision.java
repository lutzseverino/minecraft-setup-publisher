package com.lutzseverino.minecraftsetup.application;

import com.lutzseverino.minecraftsetup.domain.ChallengeCode;
import com.lutzseverino.minecraftsetup.domain.GateOutcome;
import java.util.Objects;
import java.util.Optional;

public record LoginGateDecision(GateOutcome outcome, Optional<ChallengeCode> challengeCode) {
  public LoginGateDecision {
    Objects.requireNonNull(outcome, "outcome");
    challengeCode = Objects.requireNonNull(challengeCode, "challengeCode");
    if (outcome.needsSetup() != challengeCode.isPresent()) {
      throw new IllegalArgumentException("Setup decisions must carry exactly one setup code");
    }
  }

  public static LoginGateDecision allowed(GateOutcome outcome) {
    return new LoginGateDecision(outcome, Optional.empty());
  }

  public static LoginGateDecision advisory(GateOutcome outcome, ChallengeCode code) {
    return new LoginGateDecision(outcome, Optional.of(code));
  }

  public static LoginGateDecision denied(GateOutcome outcome, Optional<ChallengeCode> code) {
    return new LoginGateDecision(outcome, code);
  }
}
