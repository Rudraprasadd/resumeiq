package com.resumeiq.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final SecretKey signingKey;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-ms}") long expirationMs,
                   @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        if (secret == null || secret.length() < 32)
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters.");
        this.signingKey           = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs         = expirationMs;
        this.refreshExpirationMs  = refreshExpirationMs;
    }

    public String generateAccessToken(UserDetails u) {
        return buildToken(Map.of("type","access"), u, expirationMs);
    }
    public String generateRefreshToken(UserDetails u) {
        return buildToken(Map.of("type","refresh"), u, refreshExpirationMs);
    }
    private String buildToken(Map<String,Object> claims, UserDetails u, long expiry) {
        return Jwts.builder().claims(claims).subject(u.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiry))
                .signWith(signingKey).compact();
    }
    public boolean isTokenValid(String token, UserDetails u) {
        try { return extractEmail(token).equals(u.getUsername()) && !isTokenExpired(token); }
        catch (JwtException e) { log.warn("JWT invalid: {}", e.getMessage()); return false; }
    }
    public boolean isTokenExpired(String token) { return extractExpiration(token).before(new Date()); }
    public String extractEmail(String token) { return extractClaim(token, Claims::getSubject); }
    public Date extractExpiration(String token) { return extractClaim(token, Claims::getExpiration); }
    public <T> T extractClaim(String token, Function<Claims,T> resolver) {
        return resolver.apply(Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).getPayload());
    }
}
