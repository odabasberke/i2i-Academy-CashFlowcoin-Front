package com.cryptopal.trading;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Maps to the {@code transaction_logs} table created in {@code db/init.sql}.
 * Append-only ledger row - written once by {@code TradingService}, never
 * updated, so only {@code @Getter} is needed (no {@code @Setter}, unlike
 * {@link Wallet}).
 *
 * <p>One row per trade, keyed to the <em>crypto</em> wallet (not the fiat
 * one) since that's the asset {@code currencyPair} refers to - the schema
 * has a single {@code wallet_id}/{@code balance_after} per row, not a
 * double-entry pair, so this is the more natural of the two wallets a
 * trade touches to associate the row with.
 */
@Entity
@Table(name = "transaction_logs")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Builder.Default
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Column(name = "currency_pair", length = 20)
    private String currencyPair;

    @Column(name = "amount", nullable = false, precision = 30, scale = 10)
    private BigDecimal amount;

    @Column(name = "price", precision = 30, scale = 10)
    private BigDecimal price;

    @Column(name = "fee", nullable = false, precision = 30, scale = 10)
    private BigDecimal fee;

    @Column(name = "balance_after", nullable = false, precision = 30, scale = 10)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
