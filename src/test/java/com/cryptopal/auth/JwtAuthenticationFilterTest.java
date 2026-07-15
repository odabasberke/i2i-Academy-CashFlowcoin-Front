package com.cryptopal.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;
    @Mock private Claims claims;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenWithActiveSession_authenticatesRequest() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, redisTemplate);
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token.here");
        when(jwtService.validateAndParse("valid.token.here")).thenReturn(Optional.of(claims));
        when(claims.getId()).thenReturn("jti-123");
        when(claims.getSubject()).thenReturn("berke");
        when(claims.get("role", String.class)).thenReturn("USER");
        when(claims.get("userId", Long.class)).thenReturn(7L);
        when(redisTemplate.hasKey("session:jti-123")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("berke");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getDetails()).isEqualTo(7L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void validTokenButRevokedSession_doesNotAuthenticate() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, redisTemplate);
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token.here");
        when(jwtService.validateAndParse("valid.token.here")).thenReturn(Optional.of(claims));
        when(claims.getId()).thenReturn("jti-123");
        when(redisTemplate.hasKey("session:jti-123")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void missingAuthorizationHeader_doesNotAuthenticate_butStillProceeds() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, redisTemplate);
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
