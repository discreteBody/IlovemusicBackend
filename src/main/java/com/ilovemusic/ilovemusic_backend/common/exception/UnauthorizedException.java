package com.ilovemusic.ilovemusic_backend.common.exception;

/**
 * Exception for unauthorized access (401)
 */
public class UnauthorizedException extends ApplicationException {
    
    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED", 401);
    }

    public UnauthorizedException(String message, String errorCode) {
        super(message, errorCode, 401);
    }
}

