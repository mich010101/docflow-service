package com.app.docflow.api.document.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record BatchGetDocumentsRequest(
        @NotEmpty List<@NotNull UUID> ids,
        @Min(0) Integer page,
        @Min(1) @Max(1000) Integer size,
        String sortBy,
        String sortDir
) {
}
