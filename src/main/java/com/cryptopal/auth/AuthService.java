package com.cryptopal.auth;

import com.cryptopal.exception.InvalidCredentialsException;
import com.cryptopal.exception.UserAlreadyExistsException;
import com.cryptopal.trading.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String SESSION_KEY_PREFIX = "session:";

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Persists the new user and grants its signup bonus wallet in one
     * transaction (ACID): if wallet creation fails, the user insert rolls
     * back too, so we can never end up with an account that has no wallet.
     */
    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("Username already taken: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.USER)
                .active(true)
                .emailVerified(false)
                .build();
        User savedUser = userRepository.save(user);

        walletService.createSignupBonusWallet(savedUser.getId());

        log.info("Registered new user: {}", savedUser.getUsername());
        return savedUser;
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
        if (!user.isActive()) {
            throw new InvalidCredentialsException("Account is disabled");
        }

        String jti = UUID.randomUUID().toString();
        String token = jwtService.generateToken(user, jti);
        cacheSession(jti, user);

        log.info("User logged in: {}", user.getUsername());
        return new AuthResponse(
                token, "Bearer", jwtService.expirationSeconds(),
                user.getPublicId(), user.getUsername(), user.getRole());
    }

    private void cacheSession(String jti, User user) {
        try {
            SessionMetadata metadata = new SessionMetadata(
                    user.getId(), user.getUsername(), user.getPublicId(), user.getRole(), Instant.now());
            String json = objectMapper.writeValueAsString(metadata);
            redisTemplate.opsForValue().set(
                    SESSION_KEY_PREFIX + jti, json, Duration.ofSeconds(jwtService.expirationSeconds()));
        } catch (Exception e) {
            // A session-cache write failure shouldn't fail the login itself -
            // the JWT is still valid and self-contained - but it does mean
            // this session can't be remotely revoked until it naturally
            // expires, so it's worth logging loudly rather than swallowing.
            log.error("Failed to cache session for user {}", user.getUsername(), e);
        }
    }
}
