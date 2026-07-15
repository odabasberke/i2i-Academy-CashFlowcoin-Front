package com.cryptopal.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketCacheSyncWorkerTest {

    @Mock private MarketDataProvider marketDataProvider;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private PriceTrendRepository priceTrendRepository;

    private MarketCacheSyncWorker worker;

    @BeforeEach
    void setUp() {
        // Plain `new ObjectMapper()` has no java.time support registered -
        // PriceTick has an Instant field, so writeValueAsString would throw,
        // MarketCacheSyncWorker's catch(Exception) would swallow it, and
        // valueOperations.set(...) below would silently never be called.
        // findAndRegisterModules() picks up JavaTimeModule from the
        // jackson-datatype-jsr310 jar already on the classpath, matching
        // what Spring Boot's autoconfigured ObjectMapper does in production.
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        worker = new MarketCacheSyncWorker(
                marketDataProvider, redisTemplate, objectMapper, priceTrendRepository);
    }

    @Test
    void syncLatestPricesToCache_writesEachTickToRedisWithTtl() {
        PriceTick btcTick = new PriceTick("BTC", new BigDecimal("65000.00"), Instant.now());
        when(marketDataProvider.getLatestPrices()).thenReturn(Map.of("BTC", btcTick));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        worker.syncLatestPricesToCache();

        verify(valueOperations).set(
                eq("market:price:BTC"), contains("\"symbol\":\"BTC\""), eq(Duration.ofSeconds(60)));
    }
}
