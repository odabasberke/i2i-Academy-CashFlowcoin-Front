package com.cryptopal.auth;

import com.cryptopal.exception.InvalidCredentialsException;
import com.cryptopal.exception.UserAlreadyExistsException;
import com.cryptopal.trading.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private WalletService walletService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // See the same fix/note in MarketCacheSyncWorkerTest - SessionMetadata
        // has an Instant field, so a bare ObjectMapper would silently break
        // the session-cache write this test verifies.
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        authService = new AuthService(
                userRepository, walletService, passwordEncoder, jwtService, redisTemplate, objectMapper);
    }

    @Test
    void register_whenUsernameTaken_throwsAndNeverPersists() {
        RegisterRequest request = new RegisterRequest("berke", "berke@example.com", "password123");
        when(userRepository.existsByUsername("berke")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
        verify(walletService, never()).createSignupBonusWallet(any());
    }

    @Test
    void register_savesHashedPassword_andCreatesSignupWallet() {
        RegisterRequest request = new RegisterRequest("berke", "berke@example.com", "password123");
        when(userRepository.existsByUsername("berke")).thenReturn(false);
        when(userRepository.existsByEmail("berke@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(42L);
            return u;
        });

        User result = authService.register(request);

        assertThat(result.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(result.getRole()).isEqualTo(UserRole.USER);
        verify(walletService).createSignupBonusWallet(42L);
    }

    @Test
    void login_withWrongPassword_throwsInvalidCredentials() {
        User user = User.builder().username("berke").passwordHash("hashed").active(true).build();
        when(userRepository.findByUsername("berke")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("berke", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_withUnknownUsername_throwsInvalidCredentials() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "whatever")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_withValidCredentials_returnsTokenAndCachesSession() {
        User user = User.builder()
                .id(1L).publicId(UUID.randomUUID()).username("berke")
                .passwordHash("hashed").role(UserRole.USER).active(true)
                .build();
        when(userRepository.findByUsername("berke")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(eq(user), anyString())).thenReturn("fake.jwt.token");
        when(jwtService.expirationSeconds()).thenReturn(3600L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthResponse response = authService.login(new LoginRequest("berke", "password123"));

        assertThat(response.accessToken()).isEqualTo("fake.jwt.token");
        assertThat(response.username()).isEqualTo("berke");
        verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
    }
}
