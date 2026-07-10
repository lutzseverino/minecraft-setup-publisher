package com.lutzseverino.minecraftsetup.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MessageSettingsTest {
    @Test
    void setupMessagesMustContainCodePlaceholder() {
        IllegalArgumentException required = assertThrows(
                IllegalArgumentException.class,
                () -> new SetupPublisherSettings.MessageSettings(
                        "Run setup",
                        "Update with {code}",
                        "Try again"
                )
        );
        IllegalArgumentException outdated = assertThrows(
                IllegalArgumentException.class,
                () -> new SetupPublisherSettings.MessageSettings(
                        "Set up with {code}",
                        "Run the update",
                        "Try again"
                )
        );

        assertEquals("Message must contain {code}: setup-required", required.getMessage());
        assertEquals("Message must contain {code}: setup-outdated", outdated.getMessage());
    }
}
