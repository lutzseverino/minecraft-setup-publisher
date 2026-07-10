package com.lutzseverino.minecraftsetup.application;

import com.lutzseverino.minecraftsetup.domain.ManifestSnapshot;

public interface ManifestSnapshotProvider {
    ManifestSnapshot current() throws ManifestUnavailableException;
}
