package com.cryptopal.exception;

/** Thrown when a requested market price can't be served from cache right now. */
public class MarketDataUnavailableException extends RuntimeException {

    public MarketDataUnavailableException(String message) {
        super(message);
    }

    public MarketDataUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
