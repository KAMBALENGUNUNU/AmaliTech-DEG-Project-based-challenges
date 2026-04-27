package com.finsafe.gateway.exception;

/**
 * Thrown when an idempotency key is reused with a different request payload.
 */
public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}