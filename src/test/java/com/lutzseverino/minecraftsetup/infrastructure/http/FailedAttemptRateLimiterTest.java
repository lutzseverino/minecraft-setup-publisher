package com.lutzseverino.minecraftsetup.infrastructure.http;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class FailedAttemptRateLimiterTest {
    @Test
    void oneClientCannotBlockAnotherClient() {
        FailedAttemptRateLimiter limiter = new FailedAttemptRateLimiter(
                Clock.fixed(Instant.parse("2026-07-10T00:00:00Z"), ZoneOffset.UTC),
                2,
                Duration.ofMinutes(1),
                100
        );

        limiter.recordFailure("client-a");
        limiter.recordFailure("client-a");

        assertFalse(limiter.permitsAttempt("client-a"));
        assertTrue(limiter.permitsAttempt("client-b"));
    }
}
