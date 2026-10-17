package com.stockflow.product.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class UpdateProductRequest {

    @Size(max = 160, message = "Product name cannot exceed 160 characters")
    private String name;

    private String description;

    @DecimalMin(value = "0.00", message = "Price cannot be negative")
    private BigDecimal price;

    private Boolean active;

    public UpdateProductRequest() {}

    public UpdateProductRequest(String name, String description, BigDecimal price, Boolean active) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.active = active;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
