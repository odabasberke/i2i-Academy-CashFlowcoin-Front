package com.cryptopal.auth;

import java.util.UUID;

public record UserResponse(UUID publicId, String username, String email, UserRole role) {

    static UserResponse from(User user) {
        return new UserResponse(user.getPublicId(), user.getUsername(), user.getEmail(), user.getRole());
    }
}
