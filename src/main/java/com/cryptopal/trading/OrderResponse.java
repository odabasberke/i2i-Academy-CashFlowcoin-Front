package com.cryptopal.trading;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID transactionId,
        OrderType type,
        String symbol,
        BigDecimal quantity,
        BigDecimal executionPrice,
        BigDecimal totalValue,
        BigDecimal newFiatBalance,
        BigDecimal newAssetBalance,
        Instant executedAt
) {
}
