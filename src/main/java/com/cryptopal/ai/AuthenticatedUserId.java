package com.cryptopal.ai;

import org.springframework.security.core.Authentication;

/**
 * Reads the authenticated user's numeric ID out of {@link Authentication}.
 *
 * <p>Deliberately duplicated from {@code trading.AuthenticatedUserId}
 * rather than shared. Both packages agree on the same convention -
 * {@code auth.JwtAuthenticationFilter} populates {@code getDetails()} with
 * this value - not on a shared class, so {@code ai} and {@code trading}
 * stay independent of each other at the type level.
 */
final class AuthenticatedUserId {

    private AuthenticatedUserId() {
    }

    static Long from(Authentication authentication) {
        return (Long) authentication.getDetails();
    }
}
