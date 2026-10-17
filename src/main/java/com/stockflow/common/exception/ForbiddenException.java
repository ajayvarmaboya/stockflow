package com.stockflow.common.exception;

public class ForbiddenException extends StockFlowException {
    public ForbiddenException(String message) {
        super(message, "FORBIDDEN");
    }
}
