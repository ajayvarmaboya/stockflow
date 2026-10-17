package com.stockflow.common.exception;

public class IdempotencyConflictException extends ConflictException {
    public IdempotencyConflictException(String message) {
        super(message, "IDEMPOTENCY_CONFLICT");
    }
}
