package com.lutzseverino.minecraftsetup.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ComplianceRecord(
        UUID playerId,
        ManifestFingerprint fingerprint,
        ProfileId profileId,
        Instant validatedAt
) {
    public ComplianceRecord {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(fingerprint, "fingerprint");
        Objects.requireNonNull(profileId, "profileId");
        Objects.requireNonNull(validatedAt, "validatedAt");
    }

    public boolean matches(ManifestFingerprint currentFingerprint) {
        return fingerprint.equals(currentFingerprint);
    }
}
