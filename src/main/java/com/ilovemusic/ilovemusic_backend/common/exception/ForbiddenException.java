package com.ilovemusic.ilovemusic_backend.common.exception;

/**
 * Exception for forbidden access (403)
 */
public class ForbiddenException extends ApplicationException {
    
    public ForbiddenException(String message) {
        super(message, "FORBIDDEN", 403);
    }

    public ForbiddenException(String message, String errorCode) {
        super(message, errorCode, 403);
    }
}

