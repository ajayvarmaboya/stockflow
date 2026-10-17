package com.stockflow.common.exception;

public class StockFlowException extends RuntimeException {
    private final String errorCode;

    public StockFlowException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public StockFlowException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
