package com.stockflow.product.api;

import com.stockflow.common.security.SecurityUtils;
import com.stockflow.inventory.api.InventoryDto;
import com.stockflow.inventory.api.StockAdjustmentDto;
import com.stockflow.inventory.api.StockAdjustmentRequest;
import com.stockflow.inventory.application.InventoryService;
import com.stockflow.product.application.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {

    private final ProductService productService;
    private final InventoryService inventoryService;

    public AdminProductController(ProductService productService, InventoryService inventoryService) {
        this.productService = productService;
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductDto created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        ProductDto updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(updated);
    }

    @GetMapping
    public ResponseEntity<Page<ProductDto>> listAllProducts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductDto> products = productService.listAllProducts(pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductDetails(@PathVariable UUID id) {
        ProductDto product = productService.getProduct(id);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/{id}/inventory")
    public ResponseEntity<InventoryDto> getProductInventory(@PathVariable UUID id) {
        InventoryDto inventory = inventoryService.getInventory(id);
        return ResponseEntity.ok(inventory);
    }

    @PostMapping("/{id}/stock-adjustments")
    public ResponseEntity<InventoryDto> adjustStock(
            @PathVariable UUID id,
            @Valid @RequestBody StockAdjustmentRequest request) {
        String actorId = SecurityUtils.getCurrentUsername();
        InventoryDto updatedInventory = inventoryService.adjustStock(id, request.getDelta(), request.getReason(), actorId);
        return ResponseEntity.ok(updatedInventory);
    }

    @GetMapping("/{id}/stock-adjustments")
    public ResponseEntity<List<StockAdjustmentDto>> getAdjustmentHistory(@PathVariable UUID id) {
        List<StockAdjustmentDto> history = inventoryService.getAdjustmentHistory(id);
        return ResponseEntity.ok(history);
    }
}
