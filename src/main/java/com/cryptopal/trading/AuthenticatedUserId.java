package com.cryptopal.trading;

import org.springframework.security.core.Authentication;

/**
 * Reads the authenticated user's numeric ID out of {@link Authentication}.
 *
 * <p>{@code auth.JwtAuthenticationFilter} populates {@code getDetails()}
 * with this value (see the comment there) - reading it from
 * {@code details} rather than {@code getPrincipal()} (which stays the
 * username, for logging/backward compatibility) keeps this package
 * decoupled from any {@code auth}-specific type. The two packages only
 * need to agree on this one convention, not share a class.
 */
final class AuthenticatedUserId {

    private AuthenticatedUserId() {
    }

    static Long from(Authentication authentication) {
        return (Long) authentication.getDetails();
    }
}
