package com.lutzseverino.minecraftsetup.application;

import com.lutzseverino.minecraftsetup.domain.ManifestSnapshot;

public interface ManifestPublisher {
    void publish(ManifestSnapshot snapshot) throws PublicationException;
}
