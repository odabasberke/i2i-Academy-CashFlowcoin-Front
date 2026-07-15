package com.cryptopal.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges {@link MarketDataProvider} (wherever prices come from) to the two
 * places they need to end up:
 * <ul>
 *   <li>Redis, overwritten every tick - the only thing the low-latency
 *       {@code /api/market/prices} endpoint ever reads from.</li>
 *   <li>PostgreSQL {@code price_trends}, as periodic OHLC snapshots for
 *       historical analysis.</li>
 * </ul>
 * The two responsibilities run on independent schedules on purpose: caching
 * must be as frequent as new ticks arrive, while persistence can - and
 * should - be much coarser-grained to keep write volume sane.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketCacheSyncWorker {

    public static final String CACHE_KEY_PREFIX = "market:price:";
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);
    private static final String SNAPSHOT_INTERVAL = "1m";

    private final MarketDataProvider marketDataProvider;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PriceTrendRepository priceTrendRepository;

    /** One OHLC accumulator per symbol, fed by every tick, flushed on each DB snapshot. */
    private final Map<String, OhlcAccumulator> accumulators = new ConcurrentHashMap<>();

    /**
     * Redis is a pure ephemeral cache: every value carries a TTL, and this
     * is the only method that writes to it. If the app stops, stale prices
     * expire on their own instead of lingering forever - historical data
     * only ever lives in PostgreSQL.
     */
    @Scheduled(fixedRate = 15_000)
    public void syncLatestPricesToCache() {
        marketDataProvider.getLatestPrices().forEach((symbol, tick) -> {
            writeToCache(symbol, tick);
            accumulators.computeIfAbsent(symbol, s -> new OhlcAccumulator()).record(tick);
        });
    }

    private void writeToCache(String symbol, PriceTick tick) {
        try {
            String json = objectMapper.writeValueAsString(tick);
            redisTemplate.opsForValue().set(CACHE_KEY_PREFIX + symbol, json, CACHE_TTL);
        } catch (Exception e) {
            // A cache write failure for one symbol (bad JSON, Redis briefly
            // down, ...) must not stop the others from being cached this cycle.
            log.error("Failed to cache price tick for {}", symbol, e);
        }
    }

    /**
     * Coarser cadence than the cache sync above - this is what actually hits
     * PostgreSQL. Each symbol is saved independently (not one shared
     * {@code @Transactional}) so a single duplicate-key hiccup for one
     * symbol can't roll back an otherwise-successful snapshot for another.
     */
    @Scheduled(fixedRate = 60_000)
    public void snapshotToPriceTrends() {
        Instant closeTime = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        Instant openTime = closeTime.minus(1, ChronoUnit.MINUTES);

        accumulators.forEach((symbol, accumulator) ->
                accumulator.flush().ifPresent(candle -> saveSnapshot(symbol, candle, openTime, closeTime)));
    }

    private void saveSnapshot(String symbol, OhlcAccumulator.Candle candle, Instant openTime, Instant closeTime) {
        try {
            PriceTrend trend = PriceTrend.builder()
                    .symbol(symbol + "USDT")
                    .intervalType(SNAPSHOT_INTERVAL)
                    .openTime(openTime)
                    .closeTime(closeTime)
                    .openPrice(candle.open())
                    .highPrice(candle.high())
                    .lowPrice(candle.low())
                    .closePrice(candle.close())
                    .volume(BigDecimal.ZERO) // simulator has no real trade volume yet
                    .build();
            priceTrendRepository.save(trend);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate price_trends snapshot skipped for {} at {}", symbol, openTime);
        } catch (Exception e) {
            log.error("Failed to persist price_trends snapshot for {}", symbol, e);
        }
    }
}
