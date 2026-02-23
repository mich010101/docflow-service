package com.app.docflow.api.document.dto;

import com.app.docflow.application.document.command.model.BatchOperationItemStatus;

import java.util.UUID;

public record BatchItemResultResponse(UUID documentId, BatchOperationItemStatus status, String message) {
}
