package com.ecom.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

// API Gateway — single entry point for all requests
// Routes to Order Service, Inventory Service, Notification Service
@RestController
@RequestMapping("/api")
public class GatewayController {

    private static final Logger log = LoggerFactory.getLogger(GatewayController.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.order-service.url:http://order-service:8081}")
    private String orderServiceUrl;

    @Value("${services.inventory-service.url:http://inventory-service:8082}")
    private String inventoryServiceUrl;

    // Health check endpoint
    @GetMapping("/health")
    public Map<String, String> health() {
        log.info("Gateway health check called");
        return Map.of(
            "status", "UP",
            "service", "api-gateway",
            "version", "1.0.0"
        );
    }

    // Route to Order Service — create order
    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> request) {
        log.info("Gateway routing POST /orders request for product: {}", request.get("product"));
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                orderServiceUrl + "/orders", request, Map.class);
            log.info("Order created successfully via gateway");
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            log.error("Failed to route order creation request: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Order service unavailable", "message", e.getMessage()));
        }
    }

    // Route to Order Service — get order
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId) {
        log.info("Gateway routing GET /orders/{} request", orderId);
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                orderServiceUrl + "/orders/" + orderId, Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            log.error("Failed to get order {}: {}", orderId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Order not found", "orderId", orderId));
        }
    }

    // Route to Inventory Service — check stock
    @GetMapping("/inventory/{productId}")
    public ResponseEntity<?> checkInventory(@PathVariable String productId) {
        log.info("Gateway routing GET /inventory/{} request", productId);
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                inventoryServiceUrl + "/inventory/" + productId, Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            log.error("Failed to check inventory for {}: {}", productId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Inventory service unavailable"));
        }
    }
}
