package com.cryptopal.trading;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Thin public API other modules call into rather than touching
 * {@link WalletRepository} directly - keeps wallet invariants (currency
 * codes, non-negative balances, the one-wallet-per-user-per-currency rule
 * enforced by the DB) owned entirely by this package.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private static final String SIGNUP_BONUS_CURRENCY = "USD";
    private static final BigDecimal SIGNUP_BONUS_MIN = new BigDecimal("100.00");
    private static final BigDecimal SIGNUP_BONUS_MAX = new BigDecimal("1000.00");

    private final WalletRepository walletRepository;

    /**
     * Called once, right after a new user is persisted (see
     * {@code auth.AuthService#register}). Grants a random demo fiat
     * balance so the account has something to trade with immediately -
     * this is simulator/demo behavior, not something a real exchange
     * would do with real money.
     */
    @Transactional
    public Wallet createSignupBonusWallet(Long userId) {
        BigDecimal bonus = randomBetween(SIGNUP_BONUS_MIN, SIGNUP_BONUS_MAX);
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .currencyCode(SIGNUP_BONUS_CURRENCY)
                .balance(bonus)
                .lockedBalance(BigDecimal.ZERO)
                .build();
        Wallet saved = walletRepository.save(wallet);
        log.info("Created signup bonus wallet for user {}: {} {}", userId, bonus, SIGNUP_BONUS_CURRENCY);
        return saved;
    }

    private BigDecimal randomBetween(BigDecimal min, BigDecimal max) {
        double fraction = ThreadLocalRandom.current().nextDouble();
        BigDecimal range = max.subtract(min);
        return min.add(range.multiply(BigDecimal.valueOf(fraction))).setScale(2, RoundingMode.HALF_UP);
    }
}
