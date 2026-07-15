package com.cryptopal.market;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OhlcAccumulatorTest {

    private final OhlcAccumulator accumulator = new OhlcAccumulator();

    @Test
    void flush_withNoTicksRecorded_returnsEmpty() {
        assertThat(accumulator.flush()).isEmpty();
    }

    @Test
    void record_tracksOpenHighLowClose_acrossMultipleTicks() {
        accumulator.record(tick("100"));
        accumulator.record(tick("105"));
        accumulator.record(tick("95"));
        accumulator.record(tick("102"));

        OhlcAccumulator.Candle candle = accumulator.flush().orElseThrow();

        assertThat(candle.open()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(candle.high()).isEqualByComparingTo(new BigDecimal("105"));
        assertThat(candle.low()).isEqualByComparingTo(new BigDecimal("95"));
        assertThat(candle.close()).isEqualByComparingTo(new BigDecimal("102"));
    }

    @Test
    void flush_resetsState_soNextWindowStartsClean() {
        accumulator.record(tick("100"));
        accumulator.flush();

        assertThat(accumulator.flush()).isEmpty();
    }

    private PriceTick tick(String price) {
        return new PriceTick("BTC", new BigDecimal(price), Instant.now());
    }
}
