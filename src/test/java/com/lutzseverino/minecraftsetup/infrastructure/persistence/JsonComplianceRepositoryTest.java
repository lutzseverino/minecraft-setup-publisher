package com.lutzseverino.minecraftsetup.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lutzseverino.minecraftsetup.application.RepositoryException;
import com.lutzseverino.minecraftsetup.domain.ComplianceRecord;
import com.lutzseverino.minecraftsetup.domain.ManifestFingerprint;
import com.lutzseverino.minecraftsetup.domain.ProfileId;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonComplianceRepositoryTest {
  @TempDir Path temporaryDirectory;

  @Test
  void exposesAWriteFailureToLaterLoginReads() throws Exception {
    Path fileAsParent = temporaryDirectory.resolve("not-a-directory");
    Files.writeString(fileAsParent, "blocked");
    JsonComplianceRepository repository =
        new JsonComplianceRepository(fileAsParent.resolve("compliance.json"));
    ComplianceRecord record =
        new ComplianceRecord(
            UUID.randomUUID(),
            new ManifestFingerprint("msm-v1-sha256:" + "a".repeat(64)),
            new ProfileId("standard"),
            Instant.parse("2026-07-10T00:00:00Z"));

    assertThrows(RepositoryException.class, () -> repository.save(record));
    assertThrows(RepositoryException.class, () -> repository.find(record.playerId()));
  }
}
