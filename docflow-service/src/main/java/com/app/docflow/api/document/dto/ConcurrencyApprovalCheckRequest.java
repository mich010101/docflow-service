package com.app.docflow.api.document.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ConcurrencyApprovalCheckRequest(
        @Min(1) @Max(256) int threads,
        @Min(1) @Max(10000) int attempts
) {
}
