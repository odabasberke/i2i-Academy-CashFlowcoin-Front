package com.cryptopal.market;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe accumulator that folds every tick seen since the last
 * {@link #flush()} into a single OHLC candle. One instance per symbol.
 *
 * <p>Uses a lock-free compare-and-swap loop ({@link AtomicReference#updateAndGet})
 * rather than {@code synchronized}, since {@link #record} is called far more
 * often (every 15s cache sync) than {@link #flush()} (every 60s snapshot).
 */
final class OhlcAccumulator {

    private final AtomicReference<Candle> current = new AtomicReference<>();

    void record(PriceTick tick) {
        current.updateAndGet(existing -> existing == null
                ? new Candle(tick.price(), tick.price(), tick.price(), tick.price())
                : new Candle(
                        existing.open(),
                        existing.high().max(tick.price()),
                        existing.low().min(tick.price()),
                        tick.price()));
    }

    /** Returns the accumulated candle and atomically resets state for the next window. */
    Optional<Candle> flush() {
        return Optional.ofNullable(current.getAndSet(null));
    }

    record Candle(BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close) {}
}
