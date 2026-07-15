package com.cryptopal.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Reads a single symbol's live price straight out of Redis - same
 * cache-key convention {@link MarketCacheSyncWorker} writes with. Used by
 * {@code ai.AiInsightService} for portfolio-context enrichment.
 *
 * <p>{@code MarketPriceController} (Module 2) and {@code trading.TradingService}
 * (Module 4) each already have their own small inline version of this same
 * read. Left as-is there rather than retrofitted onto this reader - both
 * were already shipped and working, and this project has been treating
 * each completed module as a stable foundation rather than something to
 * silently rewrite. Worth consolidating onto this one class later if you
 * want the duplication gone.
 */
@Component
@RequiredArgsConstructor
public class MarketPriceCacheReader {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<PriceTick> getCachedPrice(String symbol) {
        String json = redisTemplate.opsForValue().get(MarketCacheSyncWorker.CACHE_KEY_PREFIX + symbol.toUpperCase());
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, PriceTick.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
