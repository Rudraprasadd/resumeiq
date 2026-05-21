package com.resumeiq.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Every error from this API looks like this:
 * {
 *   "status": 400,
 *   "error": "Validation failed",
 *   "message": "email: Must be a valid email address",
 *   "path": "/api/auth/register",
 *   "timestamp": "2025-09-12T10:30:00",
 *   "fieldErrors": { "email": "Must be a valid email address" }  // optional
 * }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
    int status,
    String error,
    String message,
    String path,
    LocalDateTime timestamp,
    Map<String, String> fieldErrors      // only present on validation errors
) {
    // Convenience factory — no field errors
    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(status, error, message, path, LocalDateTime.now(), null);
    }

    // Convenience factory — with field errors (422 validation failures)
    public static ApiErrorResponse withFields(int status, String error, String message,
                                              String path, Map<String, String> fields) {
        return new ApiErrorResponse(status, error, message, path, LocalDateTime.now(), fields);
    }
}