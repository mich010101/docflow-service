package com.app.docflow.api.document.dto;

import com.app.docflow.domain.document.DocumentHistoryAction;
import com.app.docflow.domain.document.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentHistoryResponse(
        UUID id,
        DocumentHistoryAction action,
        DocumentStatus fromStatus,
        DocumentStatus toStatus,
        String message,
        Instant createdAt
) {
}
