package com.stockflow.common.exception;

public class InsufficientStockException extends ConflictException {
    public InsufficientStockException(String message) {
        super(message, "INSUFFICIENT_STOCK");
    }
}
