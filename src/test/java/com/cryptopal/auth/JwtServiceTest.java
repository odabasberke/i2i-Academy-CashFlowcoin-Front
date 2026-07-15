package com.cryptopal.auth;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String TEST_SECRET =
            "test-secret-key-at-least-32-bytes-long-for-hs256-signing!!";

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET);
        testUser = User.builder()
                .id(1L)
                .publicId(UUID.randomUUID())
                .username("berke")
                .role(UserRole.USER)
                .build();
    }

    @Test
    void generateToken_thenValidateAndParse_roundTripsClaims() {
        String jti = UUID.randomUUID().toString();
        String token = jwtService.generateToken(testUser, jti);

        Optional<Claims> parsed = jwtService.validateAndParse(token);

        assertThat(parsed).isPresent();
        Claims claims = parsed.get();
        assertThat(claims.getSubject()).isEqualTo("berke");
        assertThat(claims.getId()).isEqualTo(jti);
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }

    @Test
    void validateAndParse_withTamperedToken_returnsEmpty() {
        String token = jwtService.generateToken(testUser, UUID.randomUUID().toString());
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtService.validateAndParse(tampered)).isEmpty();
    }

    @Test
    void validateAndParse_withGarbageInput_returnsEmpty() {
        assertThat(jwtService.validateAndParse("not-a-jwt-at-all")).isEmpty();
    }
}
