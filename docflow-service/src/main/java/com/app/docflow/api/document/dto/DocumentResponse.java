package com.app.docflow.api.document.dto;

import com.app.docflow.domain.document.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String number,
        String title,
        String author,
        String content,
        DocumentStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
