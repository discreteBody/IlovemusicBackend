package com.ilovemusic.ilovemusic_backend.common.exception;

/**
 * Exception for validation errors (422)
 */
public class ValidationException extends ApplicationException {
    
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", 422);
    }

    public ValidationException(String message, String errorCode) {
        super(message, errorCode, 422);
    }
}

