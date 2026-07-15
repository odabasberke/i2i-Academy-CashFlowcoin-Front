package com.cryptopal.trading;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private TransactionLogRepository transactionLogRepository;

    private PortfolioService portfolioService;

    @BeforeEach
    void setUp() {
        portfolioService = new PortfolioService(walletRepository, transactionLogRepository);
    }

    @Test
    void getPortfolio_mapsWalletsAndTransactions() {
        Wallet usd = Wallet.builder().userId(10L).currencyCode("USD")
                .balance(new BigDecimal("500.00")).lockedBalance(BigDecimal.ZERO).build();
        Wallet btc = Wallet.builder().userId(10L).currencyCode("BTC")
                .balance(new BigDecimal("0.03")).lockedBalance(BigDecimal.ZERO).build();
        when(walletRepository.findByUserId(10L)).thenReturn(List.of(usd, btc));

        TransactionLog trade = TransactionLog.builder()
                .userId(10L).walletId(2L).type(TransactionType.BUY).status(TransactionStatus.COMPLETED)
                .currencyPair("BTC/USD").amount(new BigDecimal("0.01")).price(new BigDecimal("50000.00"))
                .fee(BigDecimal.ZERO).balanceAfter(new BigDecimal("0.03")).completedAt(Instant.now())
                .build();
        when(transactionLogRepository.findTop50ByUserIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(trade));

        PortfolioResponse portfolio = portfolioService.getPortfolio(10L);

        assertThat(portfolio.wallets()).hasSize(2);
        assertThat(portfolio.wallets())
                .anySatisfy(w -> assertThat(w.currencyCode()).isEqualTo("USD"))
                .anySatisfy(w -> assertThat(w.currencyCode()).isEqualTo("BTC"));
        assertThat(portfolio.recentTransactions()).hasSize(1);
        assertThat(portfolio.recentTransactions().get(0).type()).isEqualTo("BUY");
    }
}
