package com.lutzseverino.minecraftsetup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lutzseverino.minecraftsetup.application.ManifestUnavailableException;
import com.lutzseverino.minecraftsetup.infrastructure.manifest.ProtocolManifestReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProtocolConformanceTest {
  @Test
  void reproducesTheProtocolGoldenFingerprint() throws Exception {
    Path fixture = Path.of("protocol/fixtures/v1/valid/minimal-vanilla.json");

    assertEquals(
        "msm-v1-sha256:1dfce0075ec03e4bdc2bfe58b6006af3c738814b3de33f053bed5017628df0a3",
        new ProtocolManifestReader(fixture).read().fingerprint().value());
  }

  @Test
  void rejectsEverySharedInvalidFixture() throws IOException {
    try (var fixtures = Files.list(Path.of("protocol/fixtures/v1/invalid"))) {
      fixtures
          .filter(path -> path.toString().endsWith(".json"))
          .forEach(
              path ->
                  assertThrows(
                      ManifestUnavailableException.class,
                      () -> new ProtocolManifestReader(path).read(),
                      path.toString()));
    }
  }
}
