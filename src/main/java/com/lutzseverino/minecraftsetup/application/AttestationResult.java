package com.lutzseverino.minecraftsetup.application;

import com.lutzseverino.minecraftsetup.domain.ManifestFingerprint;
import java.util.Objects;
import java.util.Optional;

public record AttestationResult(Optional<ManifestFingerprint> acceptedFingerprint, Optional<AttestationError> error) {
    public AttestationResult {
        acceptedFingerprint = Objects.requireNonNull(acceptedFingerprint, "acceptedFingerprint");
        error = Objects.requireNonNull(error, "error");
        if (acceptedFingerprint.isPresent() == error.isPresent()) {
            throw new IllegalArgumentException("Attestation result must be accepted or rejected");
        }
    }

    public static AttestationResult accepted(ManifestFingerprint fingerprint) {
        return new AttestationResult(Optional.of(fingerprint), Optional.empty());
    }

    public static AttestationResult rejected(AttestationError error) {
        return new AttestationResult(Optional.empty(), Optional.of(error));
    }
}
