package com.cryptopal.trading;

import com.cryptopal.exception.InsufficientAssetException;
import com.cryptopal.exception.InsufficientFundsException;
import com.cryptopal.exception.InvalidAssetException;
import com.cryptopal.exception.MarketDataUnavailableException;
import com.cryptopal.market.MarketDataProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradingServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private TransactionLogRepository transactionLogRepository;
    @Mock private MarketDataProvider marketDataProvider;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private TradingService tradingService;

    @BeforeEach
    void setUp() {
        // findAndRegisterModules(): PriceTick has an Instant field - see the
        // note in MarketCacheSyncWorkerTest for why a bare ObjectMapper
        // would silently break price deserialization here.
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        tradingService = new TradingService(
                walletRepository, transactionLogRepository, marketDataProvider, redisTemplate, objectMapper);
    }

    private void stubPrice(String symbol, String price) {
        when(marketDataProvider.getSupportedSymbols()).thenReturn(Set.of("BTC", "ETH"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("market:price:" + symbol))
                .thenReturn("{\"symbol\":\"%s\",\"price\":%s,\"timestamp\":\"2026-07-14T00:00:00Z\"}"
                        .formatted(symbol, price));
    }

    private void stubSaves() {
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionLogRepository.save(any(TransactionLog.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void executeOrder_buy_withSufficientFunds_deductsFiatAndCreditsNewAssetWallet() {
        stubPrice("BTC", "50000.00");
        stubSaves();
        Wallet fiatWallet = Wallet.builder().id(1L).userId(10L).currencyCode("USD")
                .balance(new BigDecimal("1000.00")).lockedBalance(BigDecimal.ZERO).build();
        when(walletRepository.findByUserIdAndCurrencyCodeForUpdate(10L, "USD")).thenReturn(Optional.of(fiatWallet));
        when(walletRepository.findByUserIdAndCurrencyCodeForUpdate(10L, "BTC")).thenReturn(Optional.empty());

        OrderResponse response = tradingService.executeOrder(10L, new OrderRequest(OrderType.BUY, "BTC", new BigDecimal("0.01")));

        assertThat(response.newFiatBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(response.newAssetBalance()).isEqualByComparingTo(new BigDecimal("0.01"));
        assertThat(response.type()).isEqualTo(OrderType.BUY);
    }

    @Test
    void executeOrder_buy_addsToExistingAssetWallet() {
        stubPrice("BTC", "50000.00");
        stubSaves();
        Wallet fiatWallet = Wallet.builder().id(1L).userId(10L).currencyCode("USD")
                .balance(new BigDecimal("1000.00")).lockedBalance(BigDecimal.ZERO).build();
        Wallet btcWallet = Wallet.builder().id(2L).userId(10L).currencyCode("BTC")
                .balance(new BigDecimal("0.02")).lockedBalance(BigDecimal.ZERO).build();
        when(walletRepository.findByUserIdAndCurrencyCodeForUpdate(10L, "USD")).thenReturn(Optional.of(fiatWallet));
        when(walletRepository.findByUserIdAndCurrencyCodeForUpdate(10L, "BTC")).thenReturn(Optional.of(btcWallet));

        OrderResponse response = tradingService.executeOrder(10L, new OrderRequest(OrderType.BUY, "BTC", new BigDecimal("0.01")));

        assertThat(response.newAssetBalance()).isEqualByComparingTo(new BigDecimal("0.03"));
    }

    @Test
    void executeOrder_buy_withInsufficientFunds_throwsAndNeverTouchesAssetWallet() {
        stubPrice("BTC", "50000.00");
        Wallet fiatWallet = Wallet.builder().id(1L).userId(10L).currencyCode("USD")
                .balance(new BigDecimal("10.00")).lockedBalance(BigDecimal.ZERO).build();
        when(walletRepository.findByUserIdAndCurrencyCodeForUpdate(10L, "USD")).thenReturn(Optional.of(fiatWallet));

        assertThatThrownBy(() ->
                tradingService.executeOrder(10L, new OrderRequest(OrderType.BUY, "BTC", new BigDecimal("0.01"))))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void executeOrder_sell_withSufficientAsset_creditsFiatAndDeductsAsset() {
        stubPrice("BTC", "50000.00");
        stubSaves();
        Wallet fiatWallet = Wallet.builder().id(1L).userId(10L).currencyCode("USD")
                .balance(new BigDecimal("100.00")).lockedBalance(BigDecimal.ZERO).build();
        Wallet btcWallet = Wallet.builder().id(2L).userId(10L).currencyCode("BTC")
                .balance(new BigDecimal("0.05")).lockedBalance(BigDecimal.ZERO).build();
        when(walletRepository.findByUserIdAndCurrencyCodeForUpdate(10L, "USD")).thenReturn(Optional.of(fiatWallet));
        when(walletRepository.findByUserIdAndCurrencyCodeForUpdate(10L, "BTC")).thenReturn(Optional.of(btcWallet));

        OrderResponse response = tradingService.executeOrder(10L, new OrderRequest(OrderType.SELL, "BTC", new BigDecimal("0.02")));

        assertThat(response.newAssetBalance()).isEqualByComparingTo(new BigDecimal("0.03"));
        assertThat(response.newFiatBalance()).isEqualByComparingTo(new BigDecimal("1100.00"));
    }

    @Test
    void executeOrder_sell_withNoAssetWalletAtAll_throwsInsufficientAsset() {
        stubPrice("BTC", "50000.00");
        Wallet fiatWallet = Wallet.builder().id(1L).userId(10L).currencyCode("USD")
                .balance(new BigDecimal("100.00")).lockedBalance(BigDecimal.ZERO).build();
        when(walletRepository.findByUserIdAndCurrencyCodeForUpdate(10L, "USD")).thenReturn(Optional.of(fiatWallet));
        when(walletRepository.findByUserIdAndCurrencyCodeForUpdate(10L, "BTC")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                tradingService.executeOrder(10L, new OrderRequest(OrderType.SELL, "BTC", new BigDecimal("0.02"))))
                .isInstanceOf(InsufficientAssetException.class);
    }

    @Test
    void executeOrder_sell_withInsufficientQuantity_throws() {
        stubPrice("BTC", "50000.00");
        Wallet fiatWallet = Wallet.builder().id(1L).userId(10L).currencyCode("USD")
                .balance(new BigDecimal("100.00")).lockedBalance(BigDecimal.ZERO).build();
        Wallet btcWallet = Wallet.builder().id(2L).userId(10L).currencyCode("BTC")
                .balance(new BigDecimal("0.001")).lockedBalance(BigDecimal.ZERO).build();
        when(walletRepository.findByUserIdAndCurrencyCodeForUpdate(10L, "USD")).thenReturn(Optional.of(fiatWallet));
        when(walletRepository.findByUserIdAndCurrencyCodeForUpdate(10L, "BTC")).thenReturn(Optional.of(btcWallet));

        assertThatThrownBy(() ->
                tradingService.executeOrder(10L, new OrderRequest(OrderType.SELL, "BTC", new BigDecimal("0.02"))))
                .isInstanceOf(InsufficientAssetException.class);
    }

    @Test
    void executeOrder_withUnsupportedSymbol_throwsInvalidAssetBeforeTouchingWallets() {
        when(marketDataProvider.getSupportedSymbols()).thenReturn(Set.of("BTC", "ETH"));

        assertThatThrownBy(() ->
                tradingService.executeOrder(10L, new OrderRequest(OrderType.BUY, "DOGE", new BigDecimal("10"))))
                .isInstanceOf(InvalidAssetException.class);

        verifyNoWalletLookup();
    }

    @Test
    void executeOrder_withNoCachedPrice_throwsMarketDataUnavailable() {
        when(marketDataProvider.getSupportedSymbols()).thenReturn(Set.of("BTC", "ETH"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("market:price:BTC")).thenReturn(null);

        assertThatThrownBy(() ->
                tradingService.executeOrder(10L, new OrderRequest(OrderType.BUY, "BTC", new BigDecimal("0.01"))))
                .isInstanceOf(MarketDataUnavailableException.class);
    }

    private void verifyNoWalletLookup() {
        org.mockito.Mockito.verifyNoInteractions(walletRepository);
    }
}
