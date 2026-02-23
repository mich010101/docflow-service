package com.app.docflow.domain.document;

import java.util.Optional;
import java.util.UUID;

public interface ApprovalRegistryRepository {
    void save(ApprovalRegistryRecord record);

    Optional<ApprovalRegistryRecord> findByDocumentId(UUID documentId);

    long countByDocumentId(UUID documentId);
}
