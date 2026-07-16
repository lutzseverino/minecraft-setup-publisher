package com.lutzseverino.minecraftsetup.infrastructure.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lutzseverino.minecraftsetup.application.AttestationService;
import com.lutzseverino.minecraftsetup.application.ComplianceRepository;
import com.lutzseverino.minecraftsetup.domain.ChallengeCode;
import com.lutzseverino.minecraftsetup.domain.ComplianceRecord;
import com.lutzseverino.minecraftsetup.domain.ManifestFingerprint;
import com.lutzseverino.minecraftsetup.domain.ManifestSnapshot;
import com.lutzseverino.minecraftsetup.domain.ProfileId;
import com.lutzseverino.minecraftsetup.domain.SetupChallenge;
import com.lutzseverino.minecraftsetup.infrastructure.persistence.InMemoryChallengeRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoopbackSetupHttpServerTest {
  @Test
  void servesManifestAndRedeemsAChallengeExactlyOnce() throws Exception {
    Instant now = Instant.parse("2026-07-10T00:00:00Z");
    Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    ManifestFingerprint fingerprint = new ManifestFingerprint("msm-v1-sha256:" + "a".repeat(64));
    ProfileId profile = new ProfileId("standard");
    byte[] manifestBytes = "{\"schemaVersion\":1}".getBytes(StandardCharsets.UTF_8);
    ManifestSnapshot snapshot = new ManifestSnapshot(fingerprint, Set.of(profile), manifestBytes);
    InMemoryChallengeRepository challenges = new InMemoryChallengeRepository(clock, 100);
    ChallengeCode code = new ChallengeCode("0123456789ABCDEF");
    UUID playerId = UUID.randomUUID();
    challenges.saveReplacingPlayerChallenge(
        new SetupChallenge(playerId, code, fingerprint, now.plusSeconds(600)));
    MemoryComplianceRepository compliance = new MemoryComplianceRepository();
    AttestationService attestations =
        new AttestationService(challenges, compliance, () -> snapshot, clock);

    try (LoopbackSetupHttpServer server =
        new LoopbackSetupHttpServer("127.0.0.1", 0, false, () -> snapshot, attestations, clock)) {
      server.start();
      HttpClient client = HttpClient.newHttpClient();
      URI manifestUri =
          URI.create("http://127.0.0.1:" + server.port() + LoopbackSetupHttpServer.MANIFEST_PATH);
      HttpResponse<String> manifest =
          client.send(
              HttpRequest.newBuilder(manifestUri).GET().build(),
              HttpResponse.BodyHandlers.ofString());

      assertEquals(200, manifest.statusCode());
      assertEquals(new String(manifestBytes, StandardCharsets.UTF_8), manifest.body());
      assertEquals(
          "\"" + fingerprint.value() + "\"", manifest.headers().firstValue("ETag").orElseThrow());

      URI attestationUri =
          URI.create(
              "http://127.0.0.1:" + server.port() + LoopbackSetupHttpServer.ATTESTATION_PATH);
      String body =
          """
                    {
                      "protocolVersion": 1,
                      "challenge": "0123-4567-89AB-CDEF",
                      "manifestFingerprint": "%s",
                      "profileId": "standard",
                      "client": {"name": "test-client", "version": "1"}
                    }
                    """
              .formatted(fingerprint.value());
      HttpRequest request =
          HttpRequest.newBuilder(attestationUri)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(body))
              .build();
      HttpResponse<String> accepted = client.send(request, HttpResponse.BodyHandlers.ofString());
      HttpResponse<String> repeated = client.send(request, HttpResponse.BodyHandlers.ofString());

      assertEquals(200, accepted.statusCode());
      assertTrue(accepted.body().contains("\"status\":\"accepted\""));
      assertEquals(404, repeated.statusCode());
      assertTrue(
          repeated
              .headers()
              .firstValue("Content-Type")
              .orElseThrow()
              .startsWith("application/problem+json"));
      assertTrue(compliance.find(playerId).orElseThrow().matches(fingerprint));
    }
  }

  private static final class MemoryComplianceRepository implements ComplianceRepository {
    private final Map<UUID, ComplianceRecord> records = new HashMap<>();

    @Override
    public Optional<ComplianceRecord> find(UUID playerId) {
      return Optional.ofNullable(records.get(playerId));
    }

    @Override
    public void save(ComplianceRecord record) {
      records.put(record.playerId(), record);
    }
  }
}
