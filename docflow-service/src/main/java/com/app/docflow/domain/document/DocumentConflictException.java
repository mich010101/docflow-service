package com.app.docflow.domain.document;

public class DocumentConflictException extends RuntimeException {

    public DocumentConflictException(String message, Throwable cause) {
        super(message, cause);
    }

}
