package com.app.docflow.domain.document;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DocumentSearchCriteria(
        List<UUID> ids,
        DocumentStatus status,
        String author,
        Instant createdFrom,
        Instant createdTo
) {
}
