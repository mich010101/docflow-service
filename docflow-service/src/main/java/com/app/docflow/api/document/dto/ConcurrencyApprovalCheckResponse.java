package com.app.docflow.api.document.dto;

import com.app.docflow.domain.document.DocumentStatus;

public record ConcurrencyApprovalCheckResponse(
        int totalAttempts,
        long successCount,
        long conflictCount,
        long notFoundCount,
        long registryErrorCount,
        long errorCount,
        DocumentStatus finalStatus
) {
}
