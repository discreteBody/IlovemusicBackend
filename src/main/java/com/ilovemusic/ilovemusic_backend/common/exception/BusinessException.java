package com.ilovemusic.ilovemusic_backend.common.exception;

/**
 * Exception for business logic violations (400)
 */
public class BusinessException extends ApplicationException {
    
    public BusinessException(String message) {
        super(message, "BUSINESS_LOGIC_ERROR", 400);
    }

    public BusinessException(String message, String errorCode) {
        super(message, errorCode, 400);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, "BUSINESS_LOGIC_ERROR", 400, cause);
    }
}

