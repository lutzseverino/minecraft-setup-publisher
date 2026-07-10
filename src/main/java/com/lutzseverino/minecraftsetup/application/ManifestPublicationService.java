package com.lutzseverino.minecraftsetup.application;

import com.lutzseverino.minecraftsetup.domain.ManifestSnapshot;
import java.util.Objects;

public final class ManifestPublicationService {
    private final ManifestSnapshotLoader loader;
    private final ManifestSnapshotRegistry registry;
    private final ManifestPublisher publisher;

    public ManifestPublicationService(
            ManifestSnapshotLoader loader,
            ManifestSnapshotRegistry registry,
            ManifestPublisher publisher
    ) {
        this.loader = Objects.requireNonNull(loader, "loader");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
    }

    public ManifestSnapshot validate() throws ManifestUnavailableException {
        return loader.load();
    }

    public ManifestSnapshot publish() throws ManifestUnavailableException, PublicationException {
        ManifestSnapshot snapshot = loader.load();
        publisher.publish(snapshot);
        registry.activate(snapshot);
        return snapshot;
    }

    public ManifestSnapshot activateWithoutPublishing() throws ManifestUnavailableException {
        ManifestSnapshot snapshot = loader.load();
        registry.activate(snapshot);
        return snapshot;
    }
}
