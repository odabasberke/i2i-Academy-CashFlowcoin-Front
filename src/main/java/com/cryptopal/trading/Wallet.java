package com.cryptopal.trading;

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
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Maps to the {@code wallets} table created in {@code db/init.sql}.
 *
 * <p>Deliberately references its owner by raw {@code userId} (not a JPA
 * {@code @ManyToOne User}) - {@code trading} and {@code auth} stay
 * decoupled at the entity level, so neither package's persistence layer
 * has a compile-time dependency on the other's. Cross-module interaction
 * happens through {@link WalletService}'s public methods instead.
 *
 * <p>{@code @Setter} was added in Module 4: Module 3 only ever created new
 * wallets (insert-only), but trading needs to mutate {@code balance} on
 * every buy/sell.
 */
@Entity
@Table(name = "wallets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Builder.Default
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;

    @Column(name = "balance", nullable = false, precision = 30, scale = 10)
    private BigDecimal balance;

    @Column(name = "locked_balance", nullable = false, precision = 30, scale = 10)
    private BigDecimal lockedBalance;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;
}
