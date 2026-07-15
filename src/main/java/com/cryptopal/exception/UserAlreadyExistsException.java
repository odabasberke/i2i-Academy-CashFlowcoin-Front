package com.cryptopal.exception;

/** Thrown when registration is attempted with a username or email already in use. */
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
