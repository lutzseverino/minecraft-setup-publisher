package com.lutzseverino.minecraftsetup.application;

import com.lutzseverino.minecraftsetup.domain.ManifestFingerprint;
import com.lutzseverino.minecraftsetup.domain.SetupChallenge;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ChallengeIssuer {
  private final ChallengeRepository repository;
  private final ChallengeCodeGenerator codeGenerator;
  private final Clock clock;
  private final Duration timeToLive;

  public ChallengeIssuer(
      ChallengeRepository repository,
      ChallengeCodeGenerator codeGenerator,
      Clock clock,
      Duration timeToLive) {
    if (timeToLive.isNegative()
        || timeToLive.isZero()
        || timeToLive.compareTo(Duration.ofMinutes(15)) > 0) {
      throw new IllegalArgumentException("Challenge TTL must be between zero and 15 minutes");
    }
    this.repository = Objects.requireNonNull(repository, "repository");
    this.codeGenerator = Objects.requireNonNull(codeGenerator, "codeGenerator");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.timeToLive = timeToLive;
  }

  public synchronized SetupChallenge issue(UUID playerId, ManifestFingerprint fingerprint) {
    Instant now = clock.instant();
    return repository
        .findReusable(playerId, fingerprint, now)
        .orElseGet(
            () -> {
              com.lutzseverino.minecraftsetup.domain.ChallengeCode code;
              do {
                code = codeGenerator.next();
              } while (repository.contains(code));
              SetupChallenge challenge =
                  new SetupChallenge(playerId, code, fingerprint, now.plus(timeToLive));
              repository.saveReplacingPlayerChallenge(challenge);
              return challenge;
            });
  }
}
