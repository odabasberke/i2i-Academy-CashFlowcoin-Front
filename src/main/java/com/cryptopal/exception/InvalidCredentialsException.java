package com.cryptopal.exception;

/** Thrown on login when the username doesn't exist, the password is wrong, or the account is disabled. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
