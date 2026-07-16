package com.lutzseverino.minecraftsetup.application;

import com.lutzseverino.minecraftsetup.domain.ChallengeCode;
import com.lutzseverino.minecraftsetup.domain.ManifestFingerprint;
import com.lutzseverino.minecraftsetup.domain.ProfileId;
import java.util.Objects;

public record AttestationRequest(
    int protocolVersion,
    ChallengeCode challenge,
    ManifestFingerprint manifestFingerprint,
    ProfileId profileId,
    String clientName,
    String clientVersion) {
  public AttestationRequest {
    if (protocolVersion != 1) {
      throw new IllegalArgumentException("Unsupported attestation protocol version");
    }
    Objects.requireNonNull(challenge, "challenge");
    Objects.requireNonNull(manifestFingerprint, "manifestFingerprint");
    Objects.requireNonNull(profileId, "profileId");
    clientName = requireDiagnosticValue(clientName, "clientName");
    clientVersion = requireDiagnosticValue(clientVersion, "clientVersion");
  }

  private static String requireDiagnosticValue(String value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isBlank() || value.length() > 64) {
      throw new IllegalArgumentException("Invalid " + field);
    }
    return value;
  }
}
