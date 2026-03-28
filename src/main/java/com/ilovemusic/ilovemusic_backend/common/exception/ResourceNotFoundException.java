package com.ilovemusic.ilovemusic_backend.common.exception;

/**
 * Exception for resource not found (404)
 */
public class ResourceNotFoundException extends ApplicationException {
    
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(
            String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue),
            "RESOURCE_NOT_FOUND",
            404
        );
    }

    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", 404);
    }
}

