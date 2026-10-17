package com.stockflow.product.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateProductRequest {

    @NotBlank(message = "SKU is required")
    @Size(max = 64, message = "SKU cannot exceed 64 characters")
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(max = 160, message = "Product name cannot exceed 160 characters")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", message = "Price cannot be negative")
    private BigDecimal price;

    @Min(value = 0, message = "Initial quantity cannot be negative")
    private Integer initialQuantity = 0;

    public CreateProductRequest() {}

    public CreateProductRequest(String sku, String name, String description, BigDecimal price, Integer initialQuantity) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
        this.initialQuantity = initialQuantity != null ? initialQuantity : 0;
    }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getInitialQuantity() { return initialQuantity; }
    public void setInitialQuantity(Integer initialQuantity) { this.initialQuantity = initialQuantity; }
}
