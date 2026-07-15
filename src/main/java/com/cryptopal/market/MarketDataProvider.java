package com.cryptopal.market;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Abstraction over "wherever live crypto prices come from" - a simulator
 * today, a real exchange WebSocket/REST client later. Callers (the cache
 * sync worker, the prices controller) depend only on this interface, never
 * on a concrete implementation, so the data source can be swapped without
 * touching downstream code.
 */
public interface MarketDataProvider {

    /** Symbols this provider currently has data for, e.g. {"BTC", "ETH"}. */
    Set<String> getSupportedSymbols();

    /** Latest known price for one symbol, if this provider has ever produced one. */
    Optional<PriceTick> getLatestPrice(String symbol);

    /** Latest known price for every supported symbol. */
    Map<String, PriceTick> getLatestPrices();
}
