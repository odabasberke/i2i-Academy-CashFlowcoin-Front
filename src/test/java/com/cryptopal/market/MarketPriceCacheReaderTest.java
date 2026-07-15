package com.cryptopal.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketPriceCacheReaderTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private MarketPriceCacheReader reader;

    @BeforeEach
    void setUp() {
        reader = new MarketPriceCacheReader(redisTemplate, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void getCachedPrice_withValidJson_returnsDeserializedTick() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("market:price:BTC"))
                .thenReturn("{\"symbol\":\"BTC\",\"price\":51000.00,\"timestamp\":\"2026-07-14T00:00:00Z\"}");

        Optional<PriceTick> result = reader.getCachedPrice("BTC");

        assertThat(result).isPresent();
        assertThat(result.get().symbol()).isEqualTo("BTC");
    }

    @Test
    void getCachedPrice_withMissingKey_returnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("market:price:DOGE")).thenReturn(null);

        assertThat(reader.getCachedPrice("DOGE")).isEmpty();
    }

    @Test
    void getCachedPrice_withCorruptJson_returnsEmptyRatherThanThrowing() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("market:price:BTC")).thenReturn("not-valid-json{{{");

        assertThat(reader.getCachedPrice("BTC")).isEmpty();
    }
}
