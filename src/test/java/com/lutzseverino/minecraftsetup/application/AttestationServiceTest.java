package com.lutzseverino.minecraftsetup.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lutzseverino.minecraftsetup.domain.ChallengeCode;
import com.lutzseverino.minecraftsetup.domain.ComplianceRecord;
import com.lutzseverino.minecraftsetup.domain.ManifestFingerprint;
import com.lutzseverino.minecraftsetup.domain.ManifestSnapshot;
import com.lutzseverino.minecraftsetup.domain.ProfileId;
import com.lutzseverino.minecraftsetup.domain.SetupChallenge;
import com.lutzseverino.minecraftsetup.infrastructure.persistence.InMemoryChallengeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AttestationServiceTest {
  private static final Instant NOW = Instant.parse("2026-07-10T00:00:00Z");
  private static final ManifestFingerprint FINGERPRINT = fingerprint('a');
  private static final ProfileId PROFILE = new ProfileId("standard");

  @Test
  void recordsExactComplianceAndConsumesTheChallengeOnce() {
    InMemoryChallengeRepository challenges = challenges();
    MemoryComplianceRepository compliance = new MemoryComplianceRepository(false);
    UUID playerId = UUID.randomUUID();
    ChallengeCode code = new ChallengeCode("0123456789ABCDEF");
    challenges.saveReplacingPlayerChallenge(
        new SetupChallenge(playerId, code, FINGERPRINT, NOW.plusSeconds(600)));
    AttestationService service = service(challenges, compliance);
    AttestationRequest request = request(code, FINGERPRINT, PROFILE);

    AttestationResult first = service.redeem(request);
    AttestationResult second = service.redeem(request);

    assertEquals(FINGERPRINT, first.acceptedFingerprint().orElseThrow());
    assertEquals(AttestationError.CHALLENGE_INVALID, second.error().orElseThrow());
    assertTrue(compliance.find(playerId).orElseThrow().matches(FINGERPRINT));
  }

  @Test
  void setupChangeDoesNotConsumeOrRecordTheChallenge() {
    InMemoryChallengeRepository challenges = challenges();
    MemoryComplianceRepository compliance = new MemoryComplianceRepository(false);
    UUID playerId = UUID.randomUUID();
    ChallengeCode code = new ChallengeCode("0123456789ABCDEF");
    challenges.saveReplacingPlayerChallenge(
        new SetupChallenge(playerId, code, FINGERPRINT, NOW.plusSeconds(600)));

    AttestationResult result =
        service(challenges, compliance).redeem(request(code, fingerprint('b'), PROFILE));

    assertEquals(AttestationError.FINGERPRINT_MISMATCH, result.error().orElseThrow());
    assertTrue(compliance.find(playerId).isEmpty());
    assertTrue(challenges.contains(code));
  }

  @Test
  void storageFailureRestoresTheReservedChallengeForRetry() {
    InMemoryChallengeRepository challenges = challenges();
    MemoryComplianceRepository compliance = new MemoryComplianceRepository(true);
    ChallengeCode code = new ChallengeCode("0123456789ABCDEF");
    challenges.saveReplacingPlayerChallenge(
        new SetupChallenge(UUID.randomUUID(), code, FINGERPRINT, NOW.plusSeconds(600)));

    AttestationResult result =
        service(challenges, compliance).redeem(request(code, FINGERPRINT, PROFILE));

    assertEquals(AttestationError.ATTESTATION_UNAVAILABLE, result.error().orElseThrow());
    assertTrue(challenges.contains(code));
  }

  private static AttestationService service(
      InMemoryChallengeRepository challenges, MemoryComplianceRepository compliance) {
    ManifestSnapshot snapshot = new ManifestSnapshot(FINGERPRINT, Set.of(PROFILE), "{}".getBytes());
    return new AttestationService(
        challenges, compliance, () -> snapshot, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static InMemoryChallengeRepository challenges() {
    return new InMemoryChallengeRepository(Clock.fixed(NOW, ZoneOffset.UTC), 100);
  }

  private static AttestationRequest request(
      ChallengeCode code, ManifestFingerprint fingerprint, ProfileId profile) {
    return new AttestationRequest(1, code, fingerprint, profile, "test-client", "0.1.0");
  }

  private static ManifestFingerprint fingerprint(char character) {
    return new ManifestFingerprint("msm-v1-sha256:" + String.valueOf(character).repeat(64));
  }

  private static final class MemoryComplianceRepository implements ComplianceRepository {
    private final Map<UUID, ComplianceRecord> records = new HashMap<>();
    private final boolean failWrites;

    private MemoryComplianceRepository(boolean failWrites) {
      this.failWrites = failWrites;
    }

    @Override
    public Optional<ComplianceRecord> find(UUID playerId) {
      return Optional.ofNullable(records.get(playerId));
    }

    @Override
    public void save(ComplianceRecord record) throws RepositoryException {
      if (failWrites) {
        throw new RepositoryException("failed", new java.io.IOException("disk"));
      }
      records.put(record.playerId(), record);
    }
  }
}
