package com.resumeiq.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthRequest {

    // ---- Register ----
    public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Name must be under 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8–72 characters")
        String password
    ) {}

    // ---- Login ----
    public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        String password
    ) {}

    // ---- Refresh token ----
    public record RefreshRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
    ) {}
}