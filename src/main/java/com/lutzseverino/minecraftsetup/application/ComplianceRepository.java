package com.lutzseverino.minecraftsetup.application;

import com.lutzseverino.minecraftsetup.domain.ComplianceRecord;
import java.util.Optional;
import java.util.UUID;

public interface ComplianceRepository {
    Optional<ComplianceRecord> find(UUID playerId) throws RepositoryException;

    void save(ComplianceRecord record) throws RepositoryException;
}
