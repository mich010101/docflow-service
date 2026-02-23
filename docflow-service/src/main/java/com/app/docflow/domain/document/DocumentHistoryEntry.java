package com.app.docflow.domain.document;

import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
public final class DocumentHistoryEntry {

    private final UUID id;
    private final UUID documentId;
    private final DocumentHistoryAction action;
    private final DocumentStatus fromStatus;
    private final DocumentStatus toStatus;
    private final String message;
    private final Instant createdAt;

    private DocumentHistoryEntry(
            UUID id,
            UUID documentId,
            DocumentHistoryAction action,
            DocumentStatus fromStatus,
            DocumentStatus toStatus,
            String message,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.documentId = Objects.requireNonNull(documentId);
        this.action = Objects.requireNonNull(action);
        this.fromStatus = fromStatus;
        this.toStatus = Objects.requireNonNull(toStatus);
        this.message = message;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static DocumentHistoryEntry created(UUID documentId, Instant createdAt) {
        return new DocumentHistoryEntry(UUID.randomUUID(), documentId, DocumentHistoryAction.CREATED, null, DocumentStatus.DRAFT,
                "Document created", createdAt);
    }

    public static DocumentHistoryEntry submitted(UUID documentId, Instant createdAt) {
        return new DocumentHistoryEntry(UUID.randomUUID(), documentId, DocumentHistoryAction.SUBMITTED, DocumentStatus.DRAFT,
                DocumentStatus.SUBMITTED, "Document submitted for approval", createdAt);
    }

    public static DocumentHistoryEntry approved(UUID documentId, Instant createdAt) {
        return new DocumentHistoryEntry(UUID.randomUUID(), documentId, DocumentHistoryAction.APPROVED, DocumentStatus.SUBMITTED,
                DocumentStatus.APPROVED, "Document approved", createdAt);
    }

    public static DocumentHistoryEntry restore(
            UUID id,
            UUID documentId,
            DocumentHistoryAction action,
            DocumentStatus fromStatus,
            DocumentStatus toStatus,
            String message,
            Instant createdAt
    ) {
        return new DocumentHistoryEntry(id, documentId, action, fromStatus, toStatus, message, createdAt);
    }
}
