package com.app.docflow.api.document.dto;

import java.time.Instant;
import java.util.UUID;

public record ApprovalRegistryResponse(
        UUID id,
        String registryNumber,
        Instant approvedAt
) {
}
