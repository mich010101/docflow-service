package com.app.docflow.application.document.command.model;

import java.util.UUID;

public record BatchOperationItemResult(UUID documentId, BatchOperationItemStatus status, String message) {

    public static BatchOperationItemResult success(UUID id, String message) {
        return new BatchOperationItemResult(id, BatchOperationItemStatus.SUCCESS, message);
    }

    public static BatchOperationItemResult conflict(UUID id, String message) {
        return new BatchOperationItemResult(id, BatchOperationItemStatus.CONFLICT, message);
    }

    public static BatchOperationItemResult notFound(UUID id, String message) {
        return new BatchOperationItemResult(id, BatchOperationItemStatus.NOT_FOUND, message);
    }

    public static BatchOperationItemResult registryError(UUID id, String message) {
        return new BatchOperationItemResult(id, BatchOperationItemStatus.REGISTRY_ERROR, message);
    }

    public static BatchOperationItemResult error(UUID id, String message) {
        return new BatchOperationItemResult(id, BatchOperationItemStatus.ERROR, message);
    }

}
