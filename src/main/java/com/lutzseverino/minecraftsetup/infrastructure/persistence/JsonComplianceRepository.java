package com.lutzseverino.minecraftsetup.infrastructure.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lutzseverino.minecraftsetup.application.ComplianceRepository;
import com.lutzseverino.minecraftsetup.application.RepositoryException;
import com.lutzseverino.minecraftsetup.domain.ComplianceRecord;
import com.lutzseverino.minecraftsetup.domain.ManifestFingerprint;
import com.lutzseverino.minecraftsetup.domain.ProfileId;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class JsonComplianceRepository implements ComplianceRepository {
  private static final ObjectMapper JSON =
      new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
  private static final TypeReference<Map<String, StoredRecord>> RECORDS = new TypeReference<>() {};

  private final Path file;
  private final Map<UUID, ComplianceRecord> records;
  private boolean healthy = true;

  public JsonComplianceRepository(Path file) throws RepositoryException {
    this.file = file.toAbsolutePath().normalize();
    this.records = load();
  }

  @Override
  public synchronized Optional<ComplianceRecord> find(UUID playerId) throws RepositoryException {
    if (!healthy) {
      throw new RepositoryException(
          "Setup compliance storage is unhealthy",
          new IllegalStateException("A previous durable write failed"));
    }
    return Optional.ofNullable(records.get(playerId));
  }

  @Override
  public synchronized void save(ComplianceRecord record) throws RepositoryException {
    Map<UUID, ComplianceRecord> updated = new HashMap<>(records);
    updated.put(record.playerId(), record);
    try {
      write(updated);
    } catch (RepositoryException exception) {
      healthy = false;
      throw exception;
    }
    records.clear();
    records.putAll(updated);
  }

  private Map<UUID, ComplianceRecord> load() throws RepositoryException {
    if (!Files.exists(file)) {
      return new HashMap<>();
    }
    try {
      Map<String, StoredRecord> stored = JSON.readValue(file.toFile(), RECORDS);
      Map<UUID, ComplianceRecord> loaded = new HashMap<>();
      for (Map.Entry<String, StoredRecord> entry : stored.entrySet()) {
        UUID playerId = UUID.fromString(entry.getKey());
        StoredRecord value = entry.getValue();
        loaded.put(
            playerId,
            new ComplianceRecord(
                playerId,
                new ManifestFingerprint(value.manifestFingerprint()),
                new ProfileId(value.profileId()),
                Instant.parse(value.validatedAt())));
      }
      return loaded;
    } catch (IOException | IllegalArgumentException exception) {
      throw new RepositoryException("Could not load setup compliance records", exception);
    }
  }

  private void write(Map<UUID, ComplianceRecord> values) throws RepositoryException {
    Map<String, StoredRecord> stored = new java.util.TreeMap<>();
    values.forEach(
        (playerId, value) ->
            stored.put(
                playerId.toString(),
                new StoredRecord(
                    value.fingerprint().value(),
                    value.profileId().value(),
                    value.validatedAt().toString())));

    Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
    try {
      Files.createDirectories(file.getParent());
      JSON.writeValue(temporary.toFile(), stored);
      try {
        Files.move(
            temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException ignored) {
        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException exception) {
      try {
        Files.deleteIfExists(temporary);
      } catch (IOException ignored) {
        exception.addSuppressed(ignored);
      }
      throw new RepositoryException("Could not save setup compliance records", exception);
    }
  }

  private record StoredRecord(String manifestFingerprint, String profileId, String validatedAt) {}
}
