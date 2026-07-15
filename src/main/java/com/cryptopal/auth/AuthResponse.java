package com.cryptopal.auth;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UUID userPublicId,
        String username,
        UserRole role
) {
}
