package com.app.docflow.domain.document;

import java.util.UUID;

public class InvalidDocumentStateTransitionException extends RuntimeException {
    public InvalidDocumentStateTransitionException(UUID documentId, DocumentStatus from, DocumentStatus to) {
        super("Invalid status transition for document %s: %s -> %s".formatted(documentId, from, to));
    }
}
