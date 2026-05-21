package com.resumeiq.controller;

import com.resumeiq.dto.request.AuthRequest.LoginRequest;
import com.resumeiq.dto.request.AuthRequest.RefreshRequest;
import com.resumeiq.dto.request.AuthRequest.RegisterRequest;
import com.resumeiq.dto.response.AuthResponse.TokenResponse;
import com.resumeiq.dto.response.AuthResponse.UserInfo;
import com.resumeiq.model.User;
import com.resumeiq.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, token refresh")
public class AuthController {

    private final AuthService authService;

    // ----------------------------------------------------------------
    // POST /api/auth/register
    // ----------------------------------------------------------------

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user", description = "Creates account and returns JWT tokens")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    // ----------------------------------------------------------------
    // POST /api/auth/login
    // ----------------------------------------------------------------

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Returns access + refresh tokens")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ----------------------------------------------------------------
    // POST /api/auth/refresh
    // ----------------------------------------------------------------

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Exchange a valid refresh token for a new access token")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    // ----------------------------------------------------------------
    // GET /api/auth/me   — requires valid JWT
    // ----------------------------------------------------------------

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns the authenticated user's profile")
    public ResponseEntity<UserInfo> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(UserInfo.from(user));
    }
}