package com.cryptopal.market;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Simulated market data source. Implements {@link MarketDataProvider} so it
 * can be swapped later for a real exchange feed without touching any
 * caller.
 *
 * <p>Runs its own dedicated background thread rather than relying on
 * Spring's {@code @Scheduled} - this mirrors how a real exchange feed would
 * eventually behave (a long-lived connection/listener thread the framework
 * doesn't own) and keeps the simulator fully self-contained.
 *
 * <p>{@link #latestPrices} is a {@link ConcurrentHashMap}: the ticker
 * thread writes to it every {@value #TICK_INTERVAL_SECONDS}s while HTTP
 * request threads and the scheduler thread read from it concurrently - no
 * external locking needed.
 */
@Slf4j
@Component
public class SimulatedTickerEngine implements MarketDataProvider {

    private static final long TICK_INTERVAL_SECONDS = 15;

    /** Per-tick volatility (std. deviation of % change) - roughly 0.4% every 15s. */
    private static final double VOLATILITY = 0.004;

    /** Arbitrary simulation seeds - NOT live market data. */
    private static final Map<String, BigDecimal> SEED_PRICES = Map.of(
            "BTC", new BigDecimal("65000.00"),
            "ETH", new BigDecimal("3200.00")
    );

    private final Map<String, PriceTick> latestPrices = new ConcurrentHashMap<>();
    private ScheduledExecutorService executor;

    @PostConstruct
    void start() {
        SEED_PRICES.forEach((symbol, price) ->
                latestPrices.put(symbol, new PriceTick(symbol, price, Instant.now())));

        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ticker-engine");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleAtFixedRate(
                this::generateNextTick, TICK_INTERVAL_SECONDS, TICK_INTERVAL_SECONDS, TimeUnit.SECONDS);

        log.info("SimulatedTickerEngine started, ticking every {}s for {}",
                TICK_INTERVAL_SECONDS, SEED_PRICES.keySet());
    }

    @PreDestroy
    void stop() {
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("SimulatedTickerEngine stopped");
    }

    private void generateNextTick() {
        try {
            latestPrices.replaceAll((symbol, previous) ->
                    new PriceTick(symbol, nextRandomWalkPrice(previous.price()), Instant.now()));
        } catch (Exception e) {
            // A single bad tick must never kill the scheduler thread.
            log.error("Failed to generate ticker prices", e);
        }
    }

    /**
     * Package-private (not private) on purpose, so it's directly unit
     * testable without waiting on the real 15s scheduler.
     */
    BigDecimal nextRandomWalkPrice(BigDecimal previousPrice) {
        double changePercent = ThreadLocalRandom.current().nextGaussian() * VOLATILITY;
        BigDecimal next = previousPrice
                .multiply(BigDecimal.valueOf(1 + changePercent))
                .setScale(2, RoundingMode.HALF_UP);

        // Defensive floor: a single Gaussian sample would need to be a ~125-sigma
        // event to hit this, but it guarantees the price can never reach zero or go negative.
        BigDecimal floor = previousPrice.multiply(BigDecimal.valueOf(0.5));
        return next.max(floor);
    }

    @Override
    public Set<String> getSupportedSymbols() {
        return latestPrices.keySet();
    }

    @Override
    public Optional<PriceTick> getLatestPrice(String symbol) {
        return Optional.ofNullable(latestPrices.get(symbol));
    }

    @Override
    public Map<String, PriceTick> getLatestPrices() {
        return Map.copyOf(latestPrices);
    }
}
