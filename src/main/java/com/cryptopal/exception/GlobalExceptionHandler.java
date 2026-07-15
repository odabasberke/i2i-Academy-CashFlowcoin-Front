package com.cryptopal.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Central mapping from exceptions to HTTP responses. Domain exceptions
 * (e.g. {@link MarketDataUnavailableException}) live in this package too,
 * keeping error handling for the whole app in one place.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MarketDataUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleMarketDataUnavailable(MarketDataUnavailableException ex) {
        log.warn("Market data unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorBody(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage()));
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<Map<String, Object>> handleRedisDown(RedisConnectionFailureException ex) {
        log.error("Redis connection failure", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorBody(HttpStatus.SERVICE_UNAVAILABLE, "Price cache temporarily unavailable"));
    }

    @ExceptionHandler(AiServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleAiServiceUnavailable(AiServiceUnavailableException ex) {
        log.error("AI service unavailable: {}", ex.getMessage(), ex.getCause());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorBody(HttpStatus.SERVICE_UNAVAILABLE,
                        "AI service is temporarily unavailable. Please try again shortly."));
    }

    @ExceptionHandler(TaskRejectedException.class)
    public ResponseEntity<Map<String, Object>> handleTaskRejected(TaskRejectedException ex) {
        log.warn("AI task pool at capacity: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorBody(HttpStatus.SERVICE_UNAVAILABLE,
                        "AI service is at capacity. Please try again shortly."));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.warn("Registration conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("Login failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorBody(HttpStatus.UNAUTHORIZED, ex.getMessage()));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientFunds(InsufficientFundsException ex) {
        log.warn("Order rejected - insufficient funds: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(InsufficientAssetException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientAsset(InsufficientAssetException ex) {
        log.warn("Order rejected - insufficient asset balance: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(InvalidAssetException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidAsset(InvalidAssetException ex) {
        log.warn("Order rejected - invalid asset: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(HttpStatus.BAD_REQUEST, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error"));
    }

    private Map<String, Object> errorBody(HttpStatus status, String message) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        );
    }
}
