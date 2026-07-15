package com.cryptopal.exception;

/** Thrown when the Gemini API call fails, times out, or the AI task pool is at capacity. */
public class AiServiceUnavailableException extends RuntimeException {

    public AiServiceUnavailableException(String message) {
        super(message);
    }

    public AiServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
