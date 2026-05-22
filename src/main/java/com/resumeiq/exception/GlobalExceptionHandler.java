package com.resumeiq.exception;

import com.resumeiq.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 400 — Validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (first, second) -> first));

        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.withFields(400, "Validation failed",
                        "One or more fields are invalid", req.getRequestURI(), fieldErrors));
    }

    // 400 — Bad argument (PDF issues, etc.)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(400, "Bad request", ex.getMessage(), req.getRequestURI()));
    }

    // 401 — Bad credentials / invalid token
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(401, "Unauthorized", "Invalid email or password", req.getRequestURI()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidToken(
            InvalidTokenException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(401, "Unauthorized", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(
            UsernameNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(401, "Unauthorized", "Invalid email or password", req.getRequestURI()));
    }

    // 402 — Quota exceeded (FREE tier limit)
    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleQuotaExceeded(
            QuotaExceededException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(ApiErrorResponse.of(402, "Quota exceeded", ex.getMessage(), req.getRequestURI()));
    }

    // 404 — Resource not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(404, "Not found", ex.getMessage(), req.getRequestURI()));
    }

    // 409 — Email already exists
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailExists(
            EmailAlreadyExistsException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(409, "Conflict", ex.getMessage(), req.getRequestURI()));
    }

    // 413 — File too large
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleFileTooLarge(
            MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiErrorResponse.of(413, "File too large", "Maximum upload size is 5MB", req.getRequestURI()));
    }

    // 500 — Catch-all
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception at [{}]: {}", req.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(500, "Internal server error",
                        "Something went wrong. Please try again later.", req.getRequestURI()));
    }
}