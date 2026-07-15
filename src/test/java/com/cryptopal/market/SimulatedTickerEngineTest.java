package com.cryptopal.market;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedTickerEngineTest {

    private final SimulatedTickerEngine engine = new SimulatedTickerEngine();

    @AfterEach
    void tearDown() {
        // Safe to call even if start() was never invoked in a given test.
        engine.stop();
    }

    @Test
    void start_seedsInitialPricesForAllSupportedSymbols() {
        engine.start();

        assertThat(engine.getSupportedSymbols()).contains("BTC", "ETH");
        assertThat(engine.getLatestPrice("BTC")).isPresent();
        assertThat(engine.getLatestPrice("ETH")).isPresent();
    }

    @Test
    void getLatestPrice_forUnknownSymbol_returnsEmpty() {
        engine.start();

        assertThat(engine.getLatestPrice("DOGE")).isEmpty();
    }

    @Test
    void nextRandomWalkPrice_neverDropsBelowHalfOfPreviousPrice() {
        BigDecimal previous = new BigDecimal("100.00");

        for (int i = 0; i < 10_000; i++) {
            BigDecimal next = engine.nextRandomWalkPrice(previous);
            assertThat(next).isGreaterThanOrEqualTo(previous.multiply(new BigDecimal("0.5")));
        }
    }

    @Test
    void nextRandomWalkPrice_isAlwaysPositive() {
        BigDecimal previous = new BigDecimal("1.00");

        for (int i = 0; i < 10_000; i++) {
            BigDecimal next = engine.nextRandomWalkPrice(previous);
            assertThat(next).isPositive();
        }
    }
}
