package com.app.docflow.api.error;

import com.app.docflow.domain.document.DocumentNotFoundException;
import com.app.docflow.domain.document.InvalidDocumentStateTransitionException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String CODE_VALIDATION_ERROR = "VALIDATION_ERROR";
    private static final String CODE_DOCUMENT_NOT_FOUND = "DOCUMENT_NOT_FOUND";
    private static final String CODE_DOCUMENT_STATUS_CONFLICT = "DOCUMENT_STATUS_CONFLICT";
    private static final String CODE_BAD_REQUEST = "BAD_REQUEST";
    private static final String CODE_INTERNAL_ERROR = "INTERNAL_ERROR";
    private static final String MESSAGE_VALIDATION_FAILED = "Validation failed";
    private static final String MESSAGE_UNEXPECTED_SERVER_ERROR = "Unexpected server error";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(CODE_VALIDATION_ERROR, MESSAGE_VALIDATION_FAILED, fieldErrors));
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(DocumentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(CODE_DOCUMENT_NOT_FOUND, ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(InvalidDocumentStateTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(InvalidDocumentStateTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(CODE_DOCUMENT_STATUS_CONFLICT, ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(CODE_BAD_REQUEST, ex.getMessage() + " [" + request.getRequestURI() + "]", Map.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(CODE_INTERNAL_ERROR, MESSAGE_UNEXPECTED_SERVER_ERROR, Map.of()));
    }

}
