package com.stockflow.common.exception;

public class ConflictException extends StockFlowException {
    public ConflictException(String message) {
        super(message, "CONFLICT");
    }

    public ConflictException(String message, String errorCode) {
        super(message, errorCode);
    }
}
