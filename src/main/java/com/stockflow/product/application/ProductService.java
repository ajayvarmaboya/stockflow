package com.stockflow.product.application;

import com.stockflow.common.exception.ConflictException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.inventory.application.InventoryService;
import com.stockflow.inventory.domain.Inventory;
import com.stockflow.inventory.persistence.InventoryRepository;
import com.stockflow.product.api.CreateProductRequest;
import com.stockflow.product.api.ProductDto;
import com.stockflow.product.api.UpdateProductRequest;
import com.stockflow.product.domain.Product;
import com.stockflow.product.persistence.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;

    public ProductService(ProductRepository productRepository, InventoryRepository inventoryRepository, InventoryService inventoryService) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public ProductDto createProduct(CreateProductRequest request) {
        String normalizedSku = Product.normalizeSku(request.getSku());
        if (productRepository.existsBySku(normalizedSku)) {
            throw new ConflictException("Product with SKU '" + normalizedSku + "' already exists", "DUPLICATE_SKU");
        }

        Product product = new Product(
                UUID.randomUUID(),
                normalizedSku,
                request.getName().trim(),
                request.getDescription(),
                request.getPrice(),
                true
        );

        product = productRepository.save(product);

        int initialStock = request.getInitialQuantity() != null ? request.getInitialQuantity() : 0;
        inventoryService.initializeStock(product.getId(), initialStock);

        return toDto(product, initialStock);
    }

    @Transactional
    public ProductDto updateProduct(UUID id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            product.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        product = productRepository.save(product);
        int currentStock = getStockOrZero(product.getId());
        return toDto(product, currentStock);
    }

    @Transactional(readOnly = true)
    public ProductDto getProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        int currentStock = getStockOrZero(product.getId());
        return toDto(product, currentStock);
    }

    @Transactional(readOnly = true)
    public ProductDto getActiveProduct(UUID id) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Active product not found with id: " + id));
        int currentStock = getStockOrZero(product.getId());
        return toDto(product, currentStock);
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> listActiveProducts(Pageable pageable) {
        return productRepository.findByActiveTrue(pageable)
                .map(product -> toDto(product, getStockOrZero(product.getId())));
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> listAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(product -> toDto(product, getStockOrZero(product.getId())));
    }

    private int getStockOrZero(UUID productId) {
        return inventoryRepository.findById(productId)
                .map(Inventory::getQuantity)
                .orElse(0);
    }

    private ProductDto toDto(Product product, int stock) {
        return new ProductDto(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.isActive(),
                stock,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
