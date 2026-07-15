package com.cryptopal.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable snapshot of a symbol's price at a point in time. Thread-safe by
 * construction - this is what flows from the ticker engine, through the
 * cache-sync worker, into Redis, and back out through the REST API.
 */
public record PriceTick(String symbol, BigDecimal price, Instant timestamp) {

    public PriceTick {
        Objects.requireNonNull(symbol, "symbol must not be null");
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
