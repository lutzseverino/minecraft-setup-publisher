package com.lutzseverino.minecraftsetup.infrastructure.manifest;

import com.lutzseverino.minecraftsetup.application.ManifestSnapshotLoader;
import com.lutzseverino.minecraftsetup.application.ManifestSnapshotRegistry;
import com.lutzseverino.minecraftsetup.application.ManifestUnavailableException;
import com.lutzseverino.minecraftsetup.domain.ManifestSnapshot;
import java.util.Objects;

public final class CachedManifestSnapshotProvider implements ManifestSnapshotLoader, ManifestSnapshotRegistry {
    private final ProtocolManifestReader reader;
    private volatile ManifestSnapshot current;

    public CachedManifestSnapshotProvider(ProtocolManifestReader reader) {
        this.reader = Objects.requireNonNull(reader, "reader");
    }

    @Override
    public ManifestSnapshot load() throws ManifestUnavailableException {
        return reader.read();
    }

    @Override
    public void activate(ManifestSnapshot snapshot) {
        current = Objects.requireNonNull(snapshot, "snapshot");
    }

    @Override
    public ManifestSnapshot current() throws ManifestUnavailableException {
        ManifestSnapshot snapshot = current;
        if (snapshot == null) {
            throw new ManifestUnavailableException("No valid manifest is loaded");
        }
        return snapshot;
    }
}
