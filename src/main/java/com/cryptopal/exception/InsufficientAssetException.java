package com.cryptopal.exception;

/** Thrown when a SELL order's quantity exceeds the user's holdings of that asset (including owning none at all). */
public class InsufficientAssetException extends RuntimeException {
    public InsufficientAssetException(String message) {
        super(message);
    }
}
