package com.lutzseverino.minecraftsetup.domain;

import java.util.Objects;
import java.util.Set;

public record ManifestSnapshot(
    ManifestFingerprint fingerprint, Set<ProfileId> profiles, byte[] publishedBytes) {
  public ManifestSnapshot {
    Objects.requireNonNull(fingerprint, "fingerprint");
    profiles = Set.copyOf(profiles);
    publishedBytes = publishedBytes.clone();
  }

  @Override
  public byte[] publishedBytes() {
    return publishedBytes.clone();
  }
}
