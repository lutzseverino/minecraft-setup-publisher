package com.lutzseverino.minecraftsetup.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lutzseverino.minecraftsetup.domain.ManifestFingerprint;
import com.lutzseverino.minecraftsetup.domain.ManifestSnapshot;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ManifestPublicationServiceTest {
    @Test
    void validationDoesNotChangeTheActiveDesiredState() throws Exception {
        ManifestSnapshot old = snapshot('a');
        ManifestSnapshot candidate = snapshot('b');
        MemoryRegistry registry = new MemoryRegistry(old);
        ManifestPublicationService service = new ManifestPublicationService(
                () -> candidate,
                registry,
                ignored -> { }
        );

        assertEquals(candidate, service.validate());
        assertEquals(old, registry.current());
    }

    @Test
    void failedPublicationKeepsThePreviousDesiredStateActive() {
        ManifestSnapshot old = snapshot('a');
        ManifestSnapshot candidate = snapshot('b');
        MemoryRegistry registry = new MemoryRegistry(old);
        ManifestPublicationService service = new ManifestPublicationService(
                () -> candidate,
                registry,
                ignored -> {
                    throw new PublicationException("failed", new java.io.IOException("disk"));
                }
        );

        assertThrows(PublicationException.class, service::publish);
        assertEquals(old, registry.currentUnchecked());
    }

    @Test
    void successfulPublicationActivatesThePublishedFingerprint() throws Exception {
        ManifestSnapshot old = snapshot('a');
        ManifestSnapshot candidate = snapshot('b');
        MemoryRegistry registry = new MemoryRegistry(old);
        ManifestPublicationService service = new ManifestPublicationService(
                () -> candidate,
                registry,
                ignored -> { }
        );

        assertEquals(candidate, service.publish());
        assertEquals(candidate, registry.current());
    }

    private static ManifestSnapshot snapshot(char value) {
        return new ManifestSnapshot(
                new ManifestFingerprint("msm-v1-sha256:" + String.valueOf(value).repeat(64)),
                Set.of(),
                "{}".getBytes()
        );
    }

    private static final class MemoryRegistry implements ManifestSnapshotRegistry {
        private ManifestSnapshot current;

        private MemoryRegistry(ManifestSnapshot current) {
            this.current = current;
        }

        @Override
        public void activate(ManifestSnapshot snapshot) {
            current = snapshot;
        }

        @Override
        public ManifestSnapshot current() throws ManifestUnavailableException {
            return current;
        }

        private ManifestSnapshot currentUnchecked() {
            return current;
        }
    }
}
