package com.cryptopal.trading;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PortfolioResponse(List<WalletBalance> wallets, List<TransactionSummary> recentTransactions) {

    public record WalletBalance(String currencyCode, BigDecimal balance, BigDecimal lockedBalance) {
    }

    public record TransactionSummary(
            UUID transactionId, String type, String currencyPair,
            BigDecimal amount, BigDecimal price, BigDecimal balanceAfter, Instant executedAt) {
    }
}
