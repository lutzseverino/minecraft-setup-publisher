package com.lutzseverino.minecraftsetup.application;

import com.lutzseverino.minecraftsetup.domain.SetupChallenge;
import java.util.Objects;
import java.util.Optional;

public record ChallengeReservation(
    ChallengeReservationStatus status, Optional<SetupChallenge> challenge) {
  public ChallengeReservation {
    Objects.requireNonNull(status, "status");
    challenge = Objects.requireNonNull(challenge, "challenge");
    if ((status == ChallengeReservationStatus.RESERVED) != challenge.isPresent()) {
      throw new IllegalArgumentException("Only a reserved result carries a challenge");
    }
  }

  public static ChallengeReservation reserved(SetupChallenge challenge) {
    return new ChallengeReservation(ChallengeReservationStatus.RESERVED, Optional.of(challenge));
  }

  public static ChallengeReservation rejected(ChallengeReservationStatus status) {
    return new ChallengeReservation(status, Optional.empty());
  }
}
