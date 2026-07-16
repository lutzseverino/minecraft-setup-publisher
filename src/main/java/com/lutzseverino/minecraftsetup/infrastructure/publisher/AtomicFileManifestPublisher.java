package com.lutzseverino.minecraftsetup.infrastructure.publisher;

import com.lutzseverino.minecraftsetup.application.ManifestPublisher;
import com.lutzseverino.minecraftsetup.application.PublicationException;
import com.lutzseverino.minecraftsetup.domain.ManifestSnapshot;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public final class AtomicFileManifestPublisher implements ManifestPublisher {
  private final Path output;

  public AtomicFileManifestPublisher(Path output) {
    this.output = Objects.requireNonNull(output, "output").toAbsolutePath().normalize();
  }

  @Override
  public void publish(ManifestSnapshot snapshot) throws PublicationException {
    Path parent = output.getParent();
    Path temporary = output.resolveSibling(output.getFileName() + ".tmp");
    try {
      Files.createDirectories(parent);
      Files.write(temporary, snapshot.publishedBytes());
      try {
        Files.move(
            temporary, output, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException ignored) {
        Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException exception) {
      try {
        Files.deleteIfExists(temporary);
      } catch (IOException ignored) {
        exception.addSuppressed(ignored);
      }
      throw new PublicationException("Could not publish manifest to " + output, exception);
    }
  }
}
