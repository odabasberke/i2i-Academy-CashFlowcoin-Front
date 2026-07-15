package com.cryptopal.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Validates the {@code Authorization: Bearer <token>} header on every
 * request. Two checks, both required: the JWT's own signature/expiry
 * (stateless, no I/O), and presence of the matching session in Redis under
 * {@code session:<jti>} - this second check is what lets logout revoke a
 * token immediately instead of waiting out its natural expiry.
 *
 * <p>Registered into the filter chain by {@code config.SecurityConfig},
 * which owns *where* this sits in the chain; this class owns *how*
 * authentication itself works.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String SESSION_KEY_PREFIX = "session:";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        extractToken(request)
                .flatMap(jwtService::validateAndParse)
                .filter(this::hasActiveSession)
                .ifPresent(this::authenticate);

        filterChain.doFilter(request, response);
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }

    private boolean hasActiveSession(Claims claims) {
        Boolean exists = redisTemplate.hasKey(SESSION_KEY_PREFIX + claims.getId());
        return Boolean.TRUE.equals(exists);
    }

    private void authenticate(Claims claims) {
        String role = claims.get("role", String.class);
        Long userId = claims.get("userId", Long.class);
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        var authToken = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
        // Principal stays the username (unchanged from Module 3 - getName()
        // keeps working the same way everywhere). userId rides along in
        // details purely as a convention other packages can read without
        // needing any auth-specific type: see trading.AuthenticatedUserId.
        authToken.setDetails(userId);
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
