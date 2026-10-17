package com.stockflow.common.config;

import com.stockflow.auth.domain.Role;
import com.stockflow.auth.domain.User;
import com.stockflow.auth.persistence.UserRepository;
import com.stockflow.inventory.application.InventoryService;
import com.stockflow.product.domain.Product;
import com.stockflow.product.persistence.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           ProductRepository productRepository,
                           InventoryService inventoryService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedProducts();
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            log.info("Seeding default demo users...");

            User admin = new User(
                    UUID.randomUUID(),
                    "admin",
                    passwordEncoder.encode("Admin@123"),
                    Role.ADMIN,
                    "Administrator"
            );
            userRepository.save(admin);

            User customer1 = new User(
                    UUID.randomUUID(),
                    "customer1",
                    passwordEncoder.encode("Customer@123"),
                    Role.CUSTOMER,
                    "Alice Customer"
            );
            userRepository.save(customer1);

            User customer2 = new User(
                    UUID.randomUUID(),
                    "customer2",
                    passwordEncoder.encode("Customer@123"),
                    Role.CUSTOMER,
                    "Bob Customer"
            );
            userRepository.save(customer2);

            log.info("Seeded users: admin/Admin@123 (ADMIN), customer1/Customer@123 (CUSTOMER), customer2/Customer@123 (CUSTOMER)");
        }
    }

    private void seedProducts() {
        if (productRepository.count() == 0) {
            log.info("Seeding initial product catalogue and inventory...");

            createProductWithStock(
                    "SKU-WIDGET-01",
                    "Premium Mechanical Keyboard",
                    "Compact 75% hot-swappable mechanical keyboard with RGB backlighting",
                    new BigDecimal("129.99"),
                    25
            );

            createProductWithStock(
                    "SKU-MOUSE-02",
                    "Wireless Ergonomic Mouse",
                    "High-precision 4000 DPI wireless mouse with silent clicks",
                    new BigDecimal("59.50"),
                    50
            );

            createProductWithStock(
                    "SKU-MONITOR-03",
                    "4K IPS Monitor 27-inch",
                    "Factory-calibrated 144Hz 4K IPS display for creators and gamers",
                    new BigDecimal("349.00"),
                    10
            );

            createProductWithStock(
                    "SKU-LIMITED-04",
                    "Collector Edition Keycap Set",
                    "Single-run PBT dye-sub keycaps — only 1 unit in stock for concurrency testing",
                    new BigDecimal("49.99"),
                    1
            );

            log.info("Initial product catalogue seeded successfully.");
        }
    }

    private void createProductWithStock(String sku, String name, String description, BigDecimal price, int stock) {
        Product product = new Product(
                UUID.randomUUID(),
                sku,
                name,
                description,
                price,
                true
        );
        product = productRepository.save(product);
        inventoryService.initializeStock(product.getId(), stock);
    }
}
