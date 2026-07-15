/**
 * Authentication and user management.
 *
 * <p>Owns: registration/login business logic ({@code AuthService}), the
 * {@code User} JPA entity/repository, JWT issuance and validation
 * ({@code JwtService}), and the {@code JwtAuthenticationFilter} that runs
 * on every request. The {@code SecurityFilterChain} bean that wires this
 * filter into Spring Security lives in {@code config} (see
 * {@code SecurityConfig}) - this package owns "how do we authenticate a
 * request", {@code config} owns "where does that logic sit in the chain".
 */
package com.cryptopal.auth;
