package com.stockflow.inventory.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StockAdjustmentRequest {

    @NotNull(message = "Delta is required")
    private Integer delta;

    @NotBlank(message = "Reason is required")
    private String reason;

    public StockAdjustmentRequest() {}

    public StockAdjustmentRequest(Integer delta, String reason) {
        this.delta = delta;
        this.reason = reason;
    }

    public Integer getDelta() { return delta; }
    public void setDelta(Integer delta) { this.delta = delta; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
