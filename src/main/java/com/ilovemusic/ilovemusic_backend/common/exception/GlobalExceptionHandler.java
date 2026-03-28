package com.ilovemusic.ilovemusic_backend.common.exception;

import com.ilovemusic.ilovemusic_backend.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global Exception Handler
 * Catches all exceptions and returns consistent error responses
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle custom ApplicationException
     */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiResponse<Object>> handleApplicationException(
            ApplicationException ex,
            WebRequest request) {
        
        log.warn("Application exception: {} - {}", ex.getErrorCode(), ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
            ex.getMessage(),
            ex.getErrorCode(),
            ex.getHttpStatus()
        );
        response.setPath(request.getDescription(false).replace("uri=", ""));
        
        return new ResponseEntity<>(response, HttpStatus.valueOf(ex.getHttpStatus()));
    }

    /**
     * Handle validation errors (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        log.warn("Validation error: {}", ex.getMessage());
        
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                    error -> error.getField(),
                    error -> error.getDefaultMessage(),
                    (existing, replacement) -> existing
                ));
        
        ApiResponse<Map<String, String>> response = ApiResponse.validationError(
            "Validation failed",
            errors
        );
        response.setPath(request.getDescription(false).replace("uri=", ""));
        
        return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Handle type mismatch exceptions
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            WebRequest request) {
        
        log.warn("Type mismatch exception: {}", ex.getMessage());
        
        String message = String.format(
            "Invalid value '%s' for parameter '%s'. Expected type: %s",
            ex.getValue(),
            ex.getName(),
            ex.getRequiredType().getSimpleName()
        );
        
        ApiResponse<Object> response = ApiResponse.error(
            message,
            "INVALID_PARAMETER_TYPE",
            400
        );
        response.setPath(request.getDescription(false).replace("uri=", ""));
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle 404 Not Found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            WebRequest request) {
        
        log.warn("Endpoint not found: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        
        ApiResponse<Object> response = ApiResponse.error(
            String.format("Endpoint %s %s not found", ex.getHttpMethod(), ex.getRequestURL()),
            "ENDPOINT_NOT_FOUND",
            404
        );
        response.setPath(request.getDescription(false).replace("uri=", ""));
        
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(
            Exception ex,
            WebRequest request) {
        
        log.error("Unexpected error occurred", ex);
        
        ApiResponse<Object> response = ApiResponse.error(
            "An unexpected error occurred",
            "INTERNAL_SERVER_ERROR",
            500
        );
        response.setPath(request.getDescription(false).replace("uri=", ""));
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {
        
        log.warn("Illegal argument: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
            ex.getMessage(),
            "INVALID_ARGUMENT",
            400
        );
        response.setPath(request.getDescription(false).replace("uri=", ""));
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}

