package com.lutzseverino.minecraftsetup.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SetupChallenge(
        UUID playerId,
        ChallengeCode code,
        ManifestFingerprint fingerprint,
        Instant expiresAt
) {
    public SetupChallenge {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(fingerprint, "fingerprint");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public boolean isExpiredAt(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
