package com.cryptopal.exception;

/** Thrown when an order requests a symbol the platform doesn't support/trade. */
public class InvalidAssetException extends RuntimeException {
    public InvalidAssetException(String message) {
        super(message);
    }
}
