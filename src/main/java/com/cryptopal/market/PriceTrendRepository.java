package com.cryptopal.market;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Kept minimal on purpose (YAGNI) - this module only writes snapshots.
 * Query methods (e.g. latest N candles for a symbol/interval) belong to
 * whichever future module adds a historical-analysis endpoint.
 */
public interface PriceTrendRepository extends JpaRepository<PriceTrend, Long> {
}
