package com.lutzseverino.minecraftsetup.application;

import com.lutzseverino.minecraftsetup.domain.ComplianceRecord;
import com.lutzseverino.minecraftsetup.domain.ManifestSnapshot;
import com.lutzseverino.minecraftsetup.domain.SetupChallenge;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class AttestationService {
  private final ChallengeRepository challenges;
  private final ComplianceRepository compliance;
  private final ManifestSnapshotProvider manifests;
  private final Clock clock;

  public AttestationService(
      ChallengeRepository challenges,
      ComplianceRepository compliance,
      ManifestSnapshotProvider manifests,
      Clock clock) {
    this.challenges = Objects.requireNonNull(challenges, "challenges");
    this.compliance = Objects.requireNonNull(compliance, "compliance");
    this.manifests = Objects.requireNonNull(manifests, "manifests");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public synchronized AttestationResult redeem(AttestationRequest request) {
    Instant now = clock.instant();
    ManifestSnapshot manifest;
    try {
      manifest = manifests.current();
    } catch (ManifestUnavailableException exception) {
      return AttestationResult.rejected(AttestationError.ATTESTATION_UNAVAILABLE);
    }
    if (!manifest.fingerprint().equals(request.manifestFingerprint())) {
      return AttestationResult.rejected(AttestationError.FINGERPRINT_MISMATCH);
    }
    if (!manifest.profiles().contains(request.profileId())) {
      return AttestationResult.rejected(AttestationError.PROFILE_INVALID);
    }

    ChallengeReservation reservation =
        challenges.reserve(request.challenge(), request.manifestFingerprint(), now);
    if (reservation.status() != ChallengeReservationStatus.RESERVED) {
      return AttestationResult.rejected(
          switch (reservation.status()) {
            case INVALID -> AttestationError.CHALLENGE_INVALID;
            case EXPIRED -> AttestationError.CHALLENGE_EXPIRED;
            case FINGERPRINT_MISMATCH -> AttestationError.FINGERPRINT_MISMATCH;
            case RESERVED -> throw new IllegalStateException("Reserved challenge is missing");
          });
    }
    SetupChallenge challenge = reservation.challenge().orElseThrow();

    try {
      compliance.save(
          new ComplianceRecord(
              challenge.playerId(), request.manifestFingerprint(), request.profileId(), now));
    } catch (RepositoryException exception) {
      challenges.restore(challenge, now);
      return AttestationResult.rejected(AttestationError.ATTESTATION_UNAVAILABLE);
    }
    return AttestationResult.accepted(request.manifestFingerprint());
  }
}
