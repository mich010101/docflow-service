package com.app.docflow.application.document.concurrency.model;

import com.app.docflow.domain.document.DocumentStatus;

public record ConcurrencyApprovalCheckResult(
        int totalAttempts,
        long successCount,
        long conflictCount,
        long notFoundCount,
        long registryErrorCount,
        long errorCount,
        DocumentStatus finalStatus
) {
}
