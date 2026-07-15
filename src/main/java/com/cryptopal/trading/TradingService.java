package com.cryptopal.trading;

import com.cryptopal.exception.InsufficientAssetException;
import com.cryptopal.exception.InsufficientFundsException;
import com.cryptopal.exception.InvalidAssetException;
import com.cryptopal.exception.MarketDataUnavailableException;
import com.cryptopal.market.MarketCacheSyncWorker;
import com.cryptopal.market.MarketDataProvider;
import com.cryptopal.market.PriceTick;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Executes market BUY/SELL orders. The whole order - balance checks, both
 * wallet updates, and the audit log row - runs in one {@code @Transactional}
 * boundary: any failure (insufficient funds, no cached price, anything
 * else) rolls every part of it back, so a half-executed trade is never
 * observable.
 *
 * <p><b>Lock ordering:</b> every order - buy or sell - locks the user's
 * fiat wallet first, then the asset wallet ({@code SELECT ... FOR UPDATE}
 * via {@code WalletRepository#findByUserIdAndCurrencyCodeForUpdate}).
 * Keeping that order identical in both directions means two concurrent
 * orders for the same user can never deadlock by each waiting on a lock
 * the other already holds. It also has a useful side effect: since every
 * order needs the fiat wallet lock first and there's exactly one fiat
 * wallet per user, that single row lock naturally serializes all of one
 * user's concurrent orders - no lost updates on either wallet.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradingService {

    private static final String FIAT_CURRENCY = "USD";

    private final WalletRepository walletRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final MarketDataProvider marketDataProvider;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse executeOrder(Long userId, OrderRequest request) {
        String symbol = request.symbol().toUpperCase();
        validateSupportedAsset(symbol);

        BigDecimal executionPrice = getCurrentPrice(symbol).price();

        return switch (request.type()) {
            case BUY -> executeBuy(userId, symbol, request.quantity(), executionPrice);
            case SELL -> executeSell(userId, symbol, request.quantity(), executionPrice);
        };
    }

    private OrderResponse executeBuy(Long userId, String symbol, BigDecimal quantity, BigDecimal price) {
        BigDecimal totalCost = quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);

        Wallet fiatWallet = requireFiatWalletForUpdate(userId);
        if (fiatWallet.getBalance().compareTo(totalCost) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds: order needs %s %s, wallet has %s"
                            .formatted(totalCost, FIAT_CURRENCY, fiatWallet.getBalance()));
        }
        Wallet assetWallet = getOrCreateAssetWalletForUpdate(userId, symbol);

        fiatWallet.setBalance(fiatWallet.getBalance().subtract(totalCost));
        assetWallet.setBalance(assetWallet.getBalance().add(quantity));
        walletRepository.save(fiatWallet);
        walletRepository.save(assetWallet);

        TransactionLog txLog = logTransaction(userId, assetWallet, TransactionType.BUY, symbol, quantity, price);

        log.info("BUY executed: user={} {} {} @ {} (cost {} {})",
                userId, quantity, symbol, price, totalCost, FIAT_CURRENCY);
        return toResponse(txLog, OrderType.BUY, symbol, quantity, price, totalCost,
                fiatWallet.getBalance(), assetWallet.getBalance());
    }

    private OrderResponse executeSell(Long userId, String symbol, BigDecimal quantity, BigDecimal price) {
        // Fiat wallet is still locked FIRST here too, even though the
        // balance check below reads the asset wallet - lock ORDER must stay
        // identical to executeBuy regardless of which balance is checked
        // first, or the deadlock-freedom guarantee above doesn't hold.
        Wallet fiatWallet = requireFiatWalletForUpdate(userId);
        Wallet assetWallet = requireAssetWalletForUpdate(userId, symbol);

        if (assetWallet.getBalance().compareTo(quantity) < 0) {
            throw new InsufficientAssetException(
                    "Insufficient %s balance: order needs %s, wallet has %s"
                            .formatted(symbol, quantity, assetWallet.getBalance()));
        }

        BigDecimal proceeds = quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);
        assetWallet.setBalance(assetWallet.getBalance().subtract(quantity));
        fiatWallet.setBalance(fiatWallet.getBalance().add(proceeds));
        walletRepository.save(assetWallet);
        walletRepository.save(fiatWallet);

        TransactionLog txLog = logTransaction(userId, assetWallet, TransactionType.SELL, symbol, quantity, price);

        log.info("SELL executed: user={} {} {} @ {} (proceeds {} {})",
                userId, quantity, symbol, price, proceeds, FIAT_CURRENCY);
        return toResponse(txLog, OrderType.SELL, symbol, quantity, price, proceeds,
                fiatWallet.getBalance(), assetWallet.getBalance());
    }

    private void validateSupportedAsset(String symbol) {
        if (!marketDataProvider.getSupportedSymbols().contains(symbol)) {
            throw new InvalidAssetException(
                    "Unsupported asset: %s (supported: %s)".formatted(symbol, marketDataProvider.getSupportedSymbols()));
        }
    }

    /** Reads Redis directly (never PostgreSQL) - same cache-key convention MarketCacheSyncWorker writes with. */
    private PriceTick getCurrentPrice(String symbol) {
        String json = redisTemplate.opsForValue().get(MarketCacheSyncWorker.CACHE_KEY_PREFIX + symbol);
        if (json == null) {
            throw new MarketDataUnavailableException("No live price cached for " + symbol);
        }
        try {
            return objectMapper.readValue(json, PriceTick.class);
        } catch (Exception e) {
            throw new MarketDataUnavailableException("Corrupt cached price data for " + symbol, e);
        }
    }

    private Wallet requireFiatWalletForUpdate(Long userId) {
        return walletRepository.findByUserIdAndCurrencyCodeForUpdate(userId, FIAT_CURRENCY)
                .orElseThrow(() -> new IllegalStateException(
                        "User %d has no fiat wallet - should never happen, WalletService creates one at signup"
                                .formatted(userId)));
    }

    private Wallet requireAssetWalletForUpdate(Long userId, String symbol) {
        return walletRepository.findByUserIdAndCurrencyCodeForUpdate(userId, symbol)
                .orElseThrow(() -> new InsufficientAssetException("You don't own any " + symbol));
    }

    private Wallet getOrCreateAssetWalletForUpdate(Long userId, String symbol) {
        return walletRepository.findByUserIdAndCurrencyCodeForUpdate(userId, symbol)
                .orElseGet(() -> Wallet.builder()
                        .userId(userId)
                        .currencyCode(symbol)
                        .balance(BigDecimal.ZERO)
                        .lockedBalance(BigDecimal.ZERO)
                        .build());
    }

    private TransactionLog logTransaction(
            Long userId, Wallet assetWallet, TransactionType type, String symbol,
            BigDecimal quantity, BigDecimal price) {
        TransactionLog txLog = TransactionLog.builder()
                .userId(userId)
                .walletId(assetWallet.getId())
                .type(type)
                .status(TransactionStatus.COMPLETED)
                .currencyPair(symbol + "/" + FIAT_CURRENCY)
                .amount(quantity)
                .price(price)
                .fee(BigDecimal.ZERO) // no fee model yet - a future module could add maker/taker fees here
                .balanceAfter(assetWallet.getBalance())
                .completedAt(Instant.now())
                .build();
        return transactionLogRepository.save(txLog);
    }

    private OrderResponse toResponse(
            TransactionLog txLog, OrderType type, String symbol, BigDecimal quantity, BigDecimal price,
            BigDecimal totalValue, BigDecimal newFiatBalance, BigDecimal newAssetBalance) {
        return new OrderResponse(
                txLog.getPublicId(), type, symbol, quantity, price, totalValue,
                newFiatBalance, newAssetBalance, txLog.getCompletedAt());
    }
}
