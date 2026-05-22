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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.expiration-ms}")
    private long accessTokenExpiryMs;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email()))
            throw new EmailAlreadyExistsException(request.email());
        User user = User.builder()
                .email(request.email().toLowerCase().trim())
                .fullName(request.fullName().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .plan(User.Plan.FREE)
                .build();
        userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());
        return issueTokens(user);
    }

    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.email().toLowerCase().trim(), request.password()));
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new InvalidTokenException("User not found"));
        log.info("User logged in: {}", user.getEmail());
        return issueTokens(user);
    }

    public TokenResponse refresh(RefreshRequest request) {
        String token = request.refreshToken();
        String email;
        try { email = jwtUtil.extractEmail(token); }
        catch (Exception e) { throw new InvalidTokenException("Invalid refresh token"); }
        if (jwtUtil.isTokenExpired(token))
            throw new InvalidTokenException("Refresh token expired. Please log in again.");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException("User not found"));
        if (!jwtUtil.isTokenValid(token, user))
            throw new InvalidTokenException("Refresh token is no longer valid");
        log.debug("Tokens refreshed for: {}", email);
        return issueTokens(user);
    }

    private TokenResponse issueTokens(User user) {
        return TokenResponse.of(
            jwtUtil.generateAccessToken(user),
            jwtUtil.generateRefreshToken(user),
            accessTokenExpiryMs / 1000, user);
    }
}
