package com.lutzseverino.minecraftsetup.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.lutzseverino.minecraftsetup.domain.ChallengeCode;
import com.lutzseverino.minecraftsetup.domain.ManifestFingerprint;
import com.lutzseverino.minecraftsetup.domain.SetupChallenge;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryChallengeRepositoryTest {
    @Test
    void prunesExpiredChallengesAndKeepsAHardCapacity() {
        Instant now = Instant.parse("2026-07-10T00:00:00Z");
        InMemoryChallengeRepository repository = new InMemoryChallengeRepository(
                Clock.fixed(now, ZoneOffset.UTC),
                2
        );
        repository.saveReplacingPlayerChallenge(challenge("0123456789ABCDEF", now.minusSeconds(1)));

        assertEquals(0, repository.size());

        repository.saveReplacingPlayerChallenge(challenge("1123456789ABCDEF", now.plusSeconds(30)));
        repository.saveReplacingPlayerChallenge(challenge("2123456789ABCDEF", now.plusSeconds(60)));
        repository.saveReplacingPlayerChallenge(challenge("3123456789ABCDEF", now.plusSeconds(90)));

        assertEquals(2, repository.size());
        assertFalse(repository.contains(new ChallengeCode("1123456789ABCDEF")));
    }

    private static SetupChallenge challenge(String code, Instant expiresAt) {
        return new SetupChallenge(
                UUID.randomUUID(),
                new ChallengeCode(code),
                new ManifestFingerprint("msm-v1-sha256:" + "a".repeat(64)),
                expiresAt
        );
    }
}
