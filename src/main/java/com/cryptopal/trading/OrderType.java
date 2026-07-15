package com.cryptopal.trading;

/**
 * Deliberately narrower than {@link TransactionType}: the order endpoint
 * only ever accepts BUY/SELL - deposits and withdrawals aren't part of
 * this flow, so they're not offered here even though the DB enum
 * ({@code tx_type}) supports them for other purposes.
 */
public enum OrderType {
    BUY, SELL
}
