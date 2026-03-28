package com.ilovemusic.ilovemusic_backend.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Generic API Response Envelope
 * Wraps all API responses with consistent success/error format
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private boolean success;
    private String message;
    private String error;
    private T data;
    private LocalDateTime timestamp;
    private String path;
    private int status;

    /**
     * Success response with data
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .status(200)
                .build();
    }

    /**
     * Success response with data and custom status
     */
    public static <T> ApiResponse<T> success(T data, String message, int status) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .status(status)
                .build();
    }

    /**
     * Success response without data
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .status(200)
                .build();
    }

    /**
     * Error response
     */
    public static <T> ApiResponse<T> error(String message, String error, int status) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(error)
                .timestamp(LocalDateTime.now())
                .status(status)
                .build();
    }

    /**
     * Error response with data
     */
    public static <T> ApiResponse<T> error(String message, String error, int status, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(error)
                .data(data)
                .timestamp(LocalDateTime.now())
                .status(status)
                .build();
    }

    /**
     * Validation error response
     */
    public static <T> ApiResponse<T> validationError(String message, T errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error("Validation failed")
                .data(errors)
                .timestamp(LocalDateTime.now())
                .status(400)
                .build();
    }
}

