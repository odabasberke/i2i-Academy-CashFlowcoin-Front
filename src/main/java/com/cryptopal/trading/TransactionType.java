package com.cryptopal.trading;

/** Mirrors the full DB vocabulary (Postgres native enum {@code tx_type}). */
public enum TransactionType {
    DEPOSIT, WITHDRAWAL, BUY, SELL, FEE
}
