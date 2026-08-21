package com.ecom.order;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    // In-memory store - simulates a database
    private static final Map<String, Order> orders = new HashMap<>();

    // Preload sample orders
    static {
        orders.put("ORD001", new Order("ORD001", "Laptop", 1, 75000.0, "DELIVERED", "CUST001"));
        orders.put("ORD002", new Order("ORD002", "Phone", 2, 45000.0, "SHIPPED", "CUST002"));
        orders.put("ORD003", new Order("ORD003", "Headphones", 1, 3500.0, "PENDING", "CUST003"));
    }

    // Metrics - these appear in Prometheus and Dynatrace
    private final Counter ordersCreated;
    private final Counter ordersFailed;
    private final Timer orderProcessingTime;

    @Value("${services.inventory-service.url:http://inventory-service:8082}")
    private String inventoryServiceUrl;

    @Value("${services.notification-service.url:http://notification-service:8083}")
    private String notificationServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // MeterRegistry is injected by Spring - used to create custom metrics
    public OrderController(MeterRegistry registry) {
        this.ordersCreated = Counter.builder("orders.created.total")
            .description("Total orders created")
            .register(registry);
        this.ordersFailed = Counter.builder("orders.failed.total")
            .description("Total orders failed")
            .register(registry);
        this.orderProcessingTime = Timer.builder("orders.processing.time")
            .description("Time to process an order")
            .register(registry);
    }

    // GET /orders/{orderId}
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId) {
        log.info("Fetching order: {}", orderId);

        // Simulate random slow query - generates latency spike in Dynatrace
        simulateRandomDelay();

        Order order = orders.get(orderId);
        if (order == null) {
            log.error("Order not found: {}", orderId);
            return ResponseEntity.notFound().build();
        }

        log.info("Order found: {} status: {}", orderId, order.getStatus());
        return ResponseEntity.ok(order);
    }

    // POST /orders - create new order
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> request) {
        return orderProcessingTime.record(() -> {
            String product = (String) request.get("product");
            int quantity = Integer.parseInt(request.getOrDefault("quantity", "1").toString());
            double amount = Double.parseDouble(request.getOrDefault("amount", "0").toString());
            String customerId = (String) request.getOrDefault("customerId", "CUST999");

            log.info("Creating order - product: {}, quantity: {}, amount: {}, customer: {}",
                product, quantity, amount, customerId);

            // Step 1 - Check inventory
            try {
                log.info("Checking inventory for product: {}", product);
                ResponseEntity<Map> inventoryResponse = restTemplate.getForEntity(
                    inventoryServiceUrl + "/inventory/" + product, Map.class);

                Map<String, Object> inventory = inventoryResponse.getBody();
                int stock = Integer.parseInt(inventory.get("stock").toString());

                if (stock < quantity) {
                    log.warn("Insufficient stock for product: {} requested: {} available: {}",
                        product, quantity, stock);
                    ordersFailed.increment();
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Insufficient stock",
                                     "requested", quantity,
                                     "available", stock));
                }
            } catch (Exception e) {
                log.error("Inventory check failed for product: {} - {}", product, e.getMessage());
                // Continue with order even if inventory check fails
            }

            // Step 2 - Create the order
            String orderId = "ORD" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            Order order = new Order(orderId, product, quantity, amount, "PENDING", customerId);
            orders.put(orderId, order);
            ordersCreated.increment();

            log.info("Order created successfully: {} for customer: {}", orderId, customerId);

            // Step 3 - Send notification
            try {
                log.info("Sending notification for order: {}", orderId);
                restTemplate.postForEntity(
                    notificationServiceUrl + "/notify",
                    Map.of("orderId", orderId, "customerId", customerId,
                           "product", product, "amount", amount),
                    Map.class);
            } catch (Exception e) {
                log.warn("Notification failed for order: {} - {}", orderId, e.getMessage());
                // Don't fail order if notification fails
            }

            return ResponseEntity.ok(order);
        });
    }

    // Health check
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "order-service");
    }

    // Simulates random delay - used to generate latency spikes for Dynatrace demo
    private void simulateRandomDelay() {
        int chance = ThreadLocalRandom.current().nextInt(100);
        if (chance < 10) { // 10% chance of slow query
            try {
                log.warn("Slow query simulation triggered");
                Thread.sleep(2000); // 2 second delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
