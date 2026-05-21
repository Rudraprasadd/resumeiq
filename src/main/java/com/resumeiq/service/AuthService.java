package com.resumeiq.service;

import com.resumeiq.dto.request.AuthRequest.LoginRequest;
import com.resumeiq.dto.request.AuthRequest.RefreshRequest;
import com.resumeiq.dto.request.AuthRequest.RegisterRequest;
import com.resumeiq.dto.response.AuthResponse.TokenResponse;
import com.resumeiq.exception.EmailAlreadyExistsException;
import com.resumeiq.exception.InvalidTokenException;
import com.resumeiq.model.User;
import com.resumeiq.repository.UserRepository;
import com.resumeiq.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.expiration-ms}")
    private long accessTokenExpiryMs;

    // ----------------------------------------------------------------
    // REGISTER
    // ----------------------------------------------------------------

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        // 1. Guard: email already taken?
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        // 2. Build and save user — password is BCrypt hashed, never stored plain
        User user = User.builder()
                .email(request.email().toLowerCase().trim())
                .fullName(request.fullName().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .plan(User.Plan.FREE)
                .build();

        userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        // 3. Issue tokens immediately — user is logged in after registration
        return issueTokens(user);
    }

    // ----------------------------------------------------------------
    // LOGIN
    // ----------------------------------------------------------------

    public TokenResponse login(LoginRequest request) {
        // This throws BadCredentialsException if email/password wrong
        // — GlobalExceptionHandler converts it to 401
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.email().toLowerCase().trim(),
                request.password()
            )
        );

        // If we reach here, credentials are valid
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new InvalidTokenException("User not found"));

        log.info("User logged in: {}", user.getEmail());
        return issueTokens(user);
    }

    // ----------------------------------------------------------------
    // REFRESH TOKEN
    // ----------------------------------------------------------------

    public TokenResponse refresh(RefreshRequest request) {
        String refreshToken = request.refreshToken();

        // Validate the refresh token and extract email
        String email;
        try {
            email = jwtUtil.extractEmail(refreshToken);
        } catch (Exception e) {
            throw new InvalidTokenException("Invalid or malformed refresh token");
        }

        if (jwtUtil.isTokenExpired(refreshToken)) {
            throw new InvalidTokenException("Refresh token has expired. Please log in again.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException("User not found"));

        if (!jwtUtil.isTokenValid(refreshToken, user)) {
            throw new InvalidTokenException("Refresh token is no longer valid");
        }

        // Issue fresh access token (keep same refresh token until it expires)
        String newAccessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);

        log.debug("Tokens refreshed for: {}", email);
        return TokenResponse.of(newAccessToken, newRefreshToken, accessTokenExpiryMs / 1000, user);
    }

    // ----------------------------------------------------------------
    // PRIVATE HELPERS
    // ----------------------------------------------------------------

    private TokenResponse issueTokens(User user) {
        String accessToken  = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        return TokenResponse.of(accessToken, refreshToken, accessTokenExpiryMs / 1000, user);
    }
}