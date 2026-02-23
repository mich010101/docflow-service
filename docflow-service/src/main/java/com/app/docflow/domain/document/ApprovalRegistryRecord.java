package com.app.docflow.domain.document;

import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
public final class ApprovalRegistryRecord {

    private final UUID id;
    private final UUID documentId;
    private final String registryNumber;
    private final Instant approvedAt;

    private ApprovalRegistryRecord(UUID id, UUID documentId, String registryNumber, Instant approvedAt) {
        this.id = Objects.requireNonNull(id);
        this.documentId = Objects.requireNonNull(documentId);
        this.registryNumber = Objects.requireNonNull(registryNumber);
        this.approvedAt = Objects.requireNonNull(approvedAt);
    }

    public static ApprovalRegistryRecord create(UUID documentId, String registryNumber, Instant approvedAt) {
        return new ApprovalRegistryRecord(UUID.randomUUID(), documentId, registryNumber, approvedAt);
    }

    public static ApprovalRegistryRecord restore(UUID id, UUID documentId, String registryNumber, Instant approvedAt) {
        return new ApprovalRegistryRecord(id, documentId, registryNumber, approvedAt);
    }
}
