package com.stockflow.common.exception;

public class UnauthorizedException extends StockFlowException {
    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED");
    }
}
