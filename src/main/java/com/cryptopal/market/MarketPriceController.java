package com.cryptopal.market;

import com.cryptopal.exception.MarketDataUnavailableException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Live price endpoints. Every method here reads Redis only - never
 * PostgreSQL/JPA - so read traffic on this path never touches the database.
 */
@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@Tag(name = "Market Data", description = "Low-latency live price endpoints backed by Redis")
public class MarketPriceController {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MarketDataProvider marketDataProvider; // in-memory symbol list only, no I/O

    @Operation(summary = "Latest cached price for every supported symbol")
    @GetMapping("/prices")
    public ResponseEntity<Map<String, PriceTick>> getLatestPrices() {
        List<String> symbols = List.copyOf(marketDataProvider.getSupportedSymbols());
        // MGET against a fixed, known key set - never KEYS/SCAN, which would
        // walk Redis's whole keyspace on every request to this hot path.
        List<String> keys = symbols.stream().map(this::cacheKey).toList();
        List<String> cachedValues = redisTemplate.opsForValue().multiGet(keys);

        Map<String, PriceTick> result = new LinkedHashMap<>();
        if (cachedValues != null) {
            for (int i = 0; i < symbols.size(); i++) {
                String json = cachedValues.get(i);
                if (json != null) {
                    result.put(symbols.get(i), deserialize(json));
                }
            }
        }
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Latest cached price for a single symbol")
    @GetMapping("/prices/{symbol}")
    public ResponseEntity<PriceTick> getLatestPrice(@PathVariable String symbol) {
        String json = redisTemplate.opsForValue().get(cacheKey(symbol.toUpperCase()));
        if (json == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(deserialize(json));
    }

    private String cacheKey(String symbol) {
        return MarketCacheSyncWorker.CACHE_KEY_PREFIX + symbol;
    }

    private PriceTick deserialize(String json) {
        try {
            return objectMapper.readValue(json, PriceTick.class);
        } catch (JsonProcessingException e) {
            throw new MarketDataUnavailableException("Corrupt cached price data", e);
        }
    }
}
