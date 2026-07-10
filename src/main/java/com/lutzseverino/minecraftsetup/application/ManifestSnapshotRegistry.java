package com.lutzseverino.minecraftsetup.application;

import com.lutzseverino.minecraftsetup.domain.ManifestSnapshot;

public interface ManifestSnapshotRegistry extends ManifestSnapshotProvider {
    void activate(ManifestSnapshot snapshot);
}
