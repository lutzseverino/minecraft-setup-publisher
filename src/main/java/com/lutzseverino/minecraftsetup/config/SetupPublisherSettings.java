package com.lutzseverino.minecraftsetup.config;

import com.lutzseverino.minecraftsetup.domain.FailurePolicy;
import com.lutzseverino.minecraftsetup.domain.GateMode;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

public record SetupPublisherSettings(
        Path manifestSource,
        boolean publishOnStart,
        Path publicationOutput,
        HttpSettings http,
        GateSettings gate,
        MessageSettings messages
) {
    public SetupPublisherSettings {
        Objects.requireNonNull(manifestSource, "manifestSource");
        Objects.requireNonNull(publicationOutput, "publicationOutput");
        Objects.requireNonNull(http, "http");
        Objects.requireNonNull(gate, "gate");
        Objects.requireNonNull(messages, "messages");
    }

    public record HttpSettings(boolean enabled, String bindAddress, int port, boolean trustForwardedFor) {
        public HttpSettings {
            Objects.requireNonNull(bindAddress, "bindAddress");
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("HTTP port must be between 1 and 65535");
            }
        }
    }

    public record GateSettings(
            GateMode mode,
            boolean identityForwardingTrusted,
            FailurePolicy failurePolicy,
            Duration challengeTtl,
            String bypassPermission
    ) {
        public GateSettings {
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(failurePolicy, "failurePolicy");
            Objects.requireNonNull(challengeTtl, "challengeTtl");
            Objects.requireNonNull(bypassPermission, "bypassPermission");
            if (bypassPermission.isBlank()) {
                throw new IllegalArgumentException("Bypass permission cannot be blank");
            }
        }
    }

    public record MessageSettings(String setupRequired, String setupOutdated, String serviceUnavailable) {
        public MessageSettings {
            setupRequired = requireMessage(setupRequired, "setup-required");
            setupOutdated = requireMessage(setupOutdated, "setup-outdated");
            serviceUnavailable = requireMessage(serviceUnavailable, "service-unavailable");
        }

        private static String requireMessage(String value, String key) {
            Objects.requireNonNull(value, key);
            if (value.isBlank()) {
                throw new IllegalArgumentException("Message cannot be blank: " + key);
            }
            return value;
        }
    }
}
