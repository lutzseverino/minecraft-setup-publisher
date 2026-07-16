package com.lutzseverino.minecraftsetup.application;

import com.lutzseverino.minecraftsetup.domain.ManifestSnapshot;

public interface ManifestSnapshotLoader {
  ManifestSnapshot load() throws ManifestUnavailableException;
}
