package com.app.docflow.api.document.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record BatchIdsRequest(@NotEmpty @Size(min = 1, max = 1000) List<@NotNull UUID> ids) {
}
