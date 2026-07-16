package com.lutzseverino.minecraftsetup.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.lutzseverino.minecraftsetup.domain.ChallengeCode;
import com.lutzseverino.minecraftsetup.domain.ManifestFingerprint;
import com.lutzseverino.minecraftsetup.domain.SetupChallenge;
import com.lutzseverino.minecraftsetup.infrastructure.persistence.InMemoryChallengeRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ChallengeIssuerTest {
  @Test
  void reusesAnUnexpiredChallengeForTheSameDesiredState() {
    Clock clock = Clock.fixed(Instant.parse("2026-07-10T00:00:00Z"), ZoneOffset.UTC);
    InMemoryChallengeRepository repository = new InMemoryChallengeRepository(clock, 100);
    AtomicInteger sequence = new AtomicInteger();
    ChallengeCodeGenerator generator =
        () ->
            sequence.getAndIncrement() == 0
                ? new ChallengeCode("0123456789ABCDEF")
                : new ChallengeCode("FEDCBA9876543210");
    ChallengeIssuer issuer =
        new ChallengeIssuer(repository, generator, clock, Duration.ofMinutes(10));
    UUID player = UUID.randomUUID();
    ManifestFingerprint fingerprint = fingerprint('a');

    SetupChallenge first = issuer.issue(player, fingerprint);
    SetupChallenge second = issuer.issue(player, fingerprint);
    SetupChallenge changed = issuer.issue(player, fingerprint('b'));

    assertEquals(first.code(), second.code());
    assertNotEquals(first.code(), changed.code());
  }

  private static ManifestFingerprint fingerprint(char character) {
    return new ManifestFingerprint("msm-v1-sha256:" + String.valueOf(character).repeat(64));
  }
}
