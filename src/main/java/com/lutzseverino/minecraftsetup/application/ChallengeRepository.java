package com.lutzseverino.minecraftsetup.application;

import com.lutzseverino.minecraftsetup.domain.ChallengeCode;
import com.lutzseverino.minecraftsetup.domain.ManifestFingerprint;
import com.lutzseverino.minecraftsetup.domain.SetupChallenge;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ChallengeRepository {
  Optional<SetupChallenge> findReusable(
      UUID playerId, ManifestFingerprint fingerprint, Instant now);

  boolean contains(ChallengeCode code);

  void saveReplacingPlayerChallenge(SetupChallenge challenge);

  ChallengeReservation reserve(ChallengeCode code, ManifestFingerprint fingerprint, Instant now);

  void restore(SetupChallenge challenge, Instant now);
}
