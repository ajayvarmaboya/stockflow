package com.stockflow.common.exception;

public class ResourceNotFoundException extends StockFlowException {
    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND");
    }
}
