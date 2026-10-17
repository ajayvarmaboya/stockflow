package com.stockflow.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private final Instant timestamp;
    private final int status;
    private final String code;
    private final String message;
    private final String path;
    private final String correlationId;
    private final Map<String, String> fieldErrors;

    public ErrorResponse(int status, String code, String message, String path, String correlationId) {
        this(status, code, message, path, correlationId, null);
    }

    public ErrorResponse(int status, String code, String message, String path, String correlationId, Map<String, String> fieldErrors) {
        this.timestamp = Instant.now();
        this.status = status;
        this.code = code;
        this.message = message;
        this.path = path;
        this.correlationId = correlationId;
        this.fieldErrors = fieldErrors;
    }

    public Instant getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public String getCorrelationId() { return correlationId; }
    public Map<String, String> getFieldErrors() { return fieldErrors; }
}
