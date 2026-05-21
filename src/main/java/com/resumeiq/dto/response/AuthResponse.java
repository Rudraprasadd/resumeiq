package com.resumeiq.dto.response;

import com.resumeiq.model.User;

import java.util.UUID;

public class AuthResponse {

    // ---- Token response (login + refresh) ----
    public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,       // always "Bearer"
        long expiresIn,         // seconds
        UserInfo user
    ) {
        public static TokenResponse of(String access, String refresh, long expiresIn, User user) {
            return new TokenResponse(
                access,
                refresh,
                "Bearer",
                expiresIn,
                UserInfo.from(user)
            );
        }
    }

    // ---- User summary (embedded in token response + /me endpoint) ----
    public record UserInfo(
        UUID id,
        String email,
        String fullName,
        String plan
    ) {
        public static UserInfo from(User user) {
            return new UserInfo(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPlan().name()
            );
        }
    }
}