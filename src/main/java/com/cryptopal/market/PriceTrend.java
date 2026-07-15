package com.cryptopal.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Maps to the {@code price_trends} table created in {@code db/init.sql}.
 * Column definitions are kept explicit (rather than relying on Hibernate's
 * implicit naming strategy) so they're unambiguous under {@code ddl-auto:
 * validate}.
 */
@Entity
@Table(name = "price_trends")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceTrend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "interval_type", nullable = false, length = 10)
    private String intervalType;

    @Column(name = "open_time", nullable = false)
    private Instant openTime;

    @Column(name = "close_time", nullable = false)
    private Instant closeTime;

    @Column(name = "open_price", nullable = false, precision = 30, scale = 10)
    private BigDecimal openPrice;

    @Column(name = "high_price", nullable = false, precision = 30, scale = 10)
    private BigDecimal highPrice;

    @Column(name = "low_price", nullable = false, precision = 30, scale = 10)
    private BigDecimal lowPrice;

    @Column(name = "close_price", nullable = false, precision = 30, scale = 10)
    private BigDecimal closePrice;

    @Column(name = "volume", nullable = false, precision = 30, scale = 10)
    private BigDecimal volume;

    // DB-side DEFAULT NOW() owns this column; Hibernate never writes to it.
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;
}
