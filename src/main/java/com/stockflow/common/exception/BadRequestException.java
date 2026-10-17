package com.stockflow.common.exception;

public class BadRequestException extends StockFlowException {
    public BadRequestException(String message) {
        super(message, "BAD_REQUEST");
    }
}
