package com.cryptopal.auth;

import java.time.Instant;
import java.util.UUID;

/**
 * Cached in Redis under {@code session:<jti>} on every login. Lets
 * {@link JwtAuthenticationFilter} do fast session validation without
 * hitting PostgreSQL, and gives logout a way to revoke a token before its
 * natural expiry (a plain JWT can't be revoked on its own).
 */
public record SessionMetadata(Long userId, String username, UUID publicId, UserRole role, Instant issuedAt) {
}
