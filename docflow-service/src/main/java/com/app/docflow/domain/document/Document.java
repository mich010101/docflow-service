package com.app.docflow.domain.document;

import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
public final class Document {

    private final UUID id;
    private final Long version;
    private final String number;
    private final String title;
    private final String author;
    private final String content;
    private final DocumentStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Document(
            UUID id,
            Long version,
            String number,
            String title,
            String author,
            String content,
            DocumentStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.version = version;
        this.number = Objects.requireNonNull(number, "number is required");
        this.title = validateText(title, "title");
        this.author = validateText(author, "author");
        this.content = content == null ? null : content.trim();
        this.status = Objects.requireNonNull(status, "status is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    public static Document createDraft(UUID id, String number, String title, String author, String content, Instant now) {
        return new Document(id, null, number, title, author, content, DocumentStatus.DRAFT, now, now);
    }

    public static Document restore(
            UUID id,
            Long version,
            String number,
            String title,
            String author,
            String content,
            DocumentStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Document(id, version, number, title, author, content, status, createdAt, updatedAt);
    }

    public Document submit(Instant now) {
        if (status != DocumentStatus.DRAFT) {
            throw new InvalidDocumentStateTransitionException(id, status, DocumentStatus.SUBMITTED);
        }

        return new Document(id, version, number, title, author, content, DocumentStatus.SUBMITTED, createdAt, now);
    }

    public Document approve(Instant now) {
        if (status != DocumentStatus.SUBMITTED) {
            throw new InvalidDocumentStateTransitionException(id, status, DocumentStatus.APPROVED);
        }

        return new Document(id, version, number, title, author, content, DocumentStatus.APPROVED, createdAt, now);
    }

    private static String validateText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return value.trim();
    }

}
