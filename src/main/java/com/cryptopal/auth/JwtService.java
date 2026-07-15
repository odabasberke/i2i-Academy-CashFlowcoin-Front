package com.cryptopal.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Issues and validates JWTs. The signing key comes entirely from the
 * {@code JWT_SECRET} environment variable - never hardcoded - matching the
 * "no secrets in code" rule established since Module 1.
 */
@Component
public class JwtService {

    private static final long EXPIRATION_MINUTES = 60;

    private final SecretKey signingKey;

    public JwtService(@Value("${cryptopal.jwt.secret}") String secret) {
        // HS256 requires a key of at least 256 bits (32 bytes); .env.example
        // documents this. A too-short secret fails fast here on startup,
        // rather than on the first login attempt.
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user, String jti) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(jti)
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("publicId", user.getPublicId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(EXPIRATION_MINUTES * 60)))
                .signWith(signingKey)
                .compact();
    }

    /** Returns the parsed claims if the token's signature and expiry are valid, empty otherwise. */
    public Optional<Claims> validateAndParse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long expirationSeconds() {
        return EXPIRATION_MINUTES * 60;
    }
}
