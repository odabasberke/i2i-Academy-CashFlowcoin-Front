package com.cryptopal.exception;

/** Thrown when a BUY order's cost exceeds the user's fiat wallet balance. */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
