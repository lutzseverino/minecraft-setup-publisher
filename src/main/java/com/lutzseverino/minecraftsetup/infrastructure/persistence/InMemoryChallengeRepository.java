package com.lutzseverino.minecraftsetup.infrastructure.persistence;

import com.lutzseverino.minecraftsetup.application.ChallengeRepository;
import com.lutzseverino.minecraftsetup.domain.ChallengeCode;
import com.lutzseverino.minecraftsetup.domain.ManifestFingerprint;
import com.lutzseverino.minecraftsetup.domain.SetupChallenge;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryChallengeRepository implements ChallengeRepository {
  private final Map<ChallengeCode, SetupChallenge> challenges = new HashMap<>();
  private final Clock clock;
  private final int maximumEntries;

  public InMemoryChallengeRepository(Clock clock, int maximumEntries) {
    if (maximumEntries < 1) {
      throw new IllegalArgumentException("Challenge capacity must be positive");
    }
    this.clock = clock;
    this.maximumEntries = maximumEntries;
  }

  @Override
  public synchronized Optional<SetupChallenge> findReusable(
      UUID playerId, ManifestFingerprint fingerprint, Instant now) {
    pruneExpired(now);
    return challenges.values().stream()
        .filter(challenge -> challenge.playerId().equals(playerId))
        .filter(challenge -> challenge.fingerprint().equals(fingerprint))
        .filter(challenge -> !challenge.isExpiredAt(now))
        .findFirst();
  }

  @Override
  public synchronized boolean contains(ChallengeCode code) {
    pruneExpired(clock.instant());
    return challenges.containsKey(code);
  }

  @Override
  public synchronized void saveReplacingPlayerChallenge(SetupChallenge challenge) {
    pruneExpired(clock.instant());
    challenges.values().removeIf(existing -> existing.playerId().equals(challenge.playerId()));
    if (challenges.size() >= maximumEntries) {
      challenges.values().stream()
          .min(java.util.Comparator.comparing(SetupChallenge::expiresAt))
          .ifPresent(oldest -> challenges.remove(oldest.code()));
    }
    challenges.put(challenge.code(), challenge);
  }

  @Override
  public synchronized com.lutzseverino.minecraftsetup.application.ChallengeReservation reserve(
      ChallengeCode code, ManifestFingerprint fingerprint, Instant now) {
    SetupChallenge challenge = challenges.get(code);
    if (challenge == null) {
      pruneExpired(now);
      return com.lutzseverino.minecraftsetup.application.ChallengeReservation.rejected(
          com.lutzseverino.minecraftsetup.application.ChallengeReservationStatus.INVALID);
    }
    if (challenge.isExpiredAt(now)) {
      challenges.remove(code);
      pruneExpired(now);
      return com.lutzseverino.minecraftsetup.application.ChallengeReservation.rejected(
          com.lutzseverino.minecraftsetup.application.ChallengeReservationStatus.EXPIRED);
    }
    if (!challenge.fingerprint().equals(fingerprint)) {
      return com.lutzseverino.minecraftsetup.application.ChallengeReservation.rejected(
          com.lutzseverino.minecraftsetup.application.ChallengeReservationStatus
              .FINGERPRINT_MISMATCH);
    }
    challenges.remove(code);
    pruneExpired(now);
    return com.lutzseverino.minecraftsetup.application.ChallengeReservation.reserved(challenge);
  }

  @Override
  public synchronized void restore(SetupChallenge challenge, Instant now) {
    pruneExpired(now);
    if (challenge.isExpiredAt(now) || challenges.containsKey(challenge.code())) {
      return;
    }
    boolean playerHasNewerChallenge =
        challenges.values().stream()
            .anyMatch(existing -> existing.playerId().equals(challenge.playerId()));
    if (!playerHasNewerChallenge && challenges.size() < maximumEntries) {
      challenges.put(challenge.code(), challenge);
    }
  }

  synchronized int size() {
    pruneExpired(clock.instant());
    return challenges.size();
  }

  private void pruneExpired(Instant now) {
    challenges.values().removeIf(challenge -> challenge.isExpiredAt(now));
  }
}
