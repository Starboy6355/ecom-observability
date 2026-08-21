package com.ecom.inventory;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    // In-memory inventory store
    private static final Map<String, Integer> inventory = new HashMap<>();

    static {
        inventory.put("Laptop", 50);
        inventory.put("Phone", 100);
        inventory.put("Headphones", 200);
        inventory.put("Tablet", 30);
        inventory.put("Keyboard", 150);
        inventory.put("Mouse", 200);
        inventory.put("Monitor", 25);
    }

    private final Counter stockChecks;
    private final Counter stockUpdates;
    private final Counter outOfStockEvents;

    public InventoryController(MeterRegistry registry) {
        this.stockChecks = Counter.builder("inventory.stock.checks.total")
            .description("Total stock checks performed")
            .register(registry);
        this.stockUpdates = Counter.builder("inventory.stock.updates.total")
            .description("Total stock updates performed")
            .register(registry);
        this.outOfStockEvents = Counter.builder("inventory.out.of.stock.total")
            .description("Total out of stock events")
            .register(registry);

        // Gauge - shows current stock level for Laptop in Dynatrace
        Gauge.builder("inventory.laptop.stock", inventory, inv -> inv.getOrDefault("Laptop", 0))
            .description("Current Laptop stock level")
            .register(registry);
    }

    // GET /inventory/{productId} - check stock
    @GetMapping("/{productId}")
    public ResponseEntity<?> checkStock(@PathVariable String productId) {
        log.info("Checking stock for product: {}", productId);
        stockChecks.increment();

        // Simulate random failure - 5% chance - generates errors in Dynatrace
        if (ThreadLocalRandom.current().nextInt(100) < 5) {
            log.error("Database connection timeout while checking stock for: {}", productId);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Database connection timeout", "product", productId));
        }

        Integer stock = inventory.get(productId);
        if (stock == null) {
            log.warn("Product not found in inventory: {}", productId);
            return ResponseEntity.notFound().build();
        }

        if (stock == 0) {
            log.warn("Product out of stock: {}", productId);
            outOfStockEvents.increment();
        }

        log.info("Stock check result - product: {} stock: {}", productId, stock);
        return ResponseEntity.ok(Map.of(
            "productId", productId,
            "stock", stock,
            "available", stock > 0
        ));
    }

    // PUT /inventory/{productId}/reduce - reduce stock after order
    @PutMapping("/{productId}/reduce")
    public ResponseEntity<?> reduceStock(@PathVariable String productId,
                                          @RequestBody Map<String, Object> request) {
        int quantity = Integer.parseInt(request.getOrDefault("quantity", "1").toString());
        log.info("Reducing stock for product: {} by: {}", productId, quantity);

        Integer currentStock = inventory.get(productId);
        if (currentStock == null) {
            log.error("Product not found: {}", productId);
            return ResponseEntity.notFound().build();
        }

        if (currentStock < quantity) {
            log.warn("Insufficient stock - product: {} requested: {} available: {}",
                productId, quantity, currentStock);
            outOfStockEvents.increment();
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Insufficient stock"));
        }

        inventory.put(productId, currentStock - quantity);
        stockUpdates.increment();
        log.info("Stock reduced - product: {} new stock: {}", productId, currentStock - quantity);

        return ResponseEntity.ok(Map.of(
            "productId", productId,
            "previousStock", currentStock,
            "newStock", currentStock - quantity
        ));
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "inventory-service");
    }
}
