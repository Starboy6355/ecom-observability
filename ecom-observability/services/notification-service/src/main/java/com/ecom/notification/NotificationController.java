package com.ecom.notification;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/notify")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final Counter notificationsSent;
    private final Counter notificationsFailed;

    public NotificationController(MeterRegistry registry) {
        this.notificationsSent = Counter.builder("notifications.sent.total")
            .description("Total notifications sent successfully")
            .register(registry);
        this.notificationsFailed = Counter.builder("notifications.failed.total")
            .description("Total notifications failed")
            .register(registry);
    }

    // POST /notify - send order notification
    @PostMapping
    public ResponseEntity<?> sendNotification(@RequestBody Map<String, Object> request) {
        String orderId = (String) request.get("orderId");
        String customerId = (String) request.get("customerId");
        String product = (String) request.get("product");
        double amount = Double.parseDouble(request.getOrDefault("amount", "0").toString());

        log.info("Sending notification for order: {} to customer: {}", orderId, customerId);

        // Simulate 15% failure rate - generates error metrics in Dynatrace
        if (ThreadLocalRandom.current().nextInt(100) < 15) {
            log.error("Email service timeout - failed to send notification for order: {}", orderId);
            notificationsFailed.increment();
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "status", "FAILED",
                    "orderId", orderId,
                    "reason", "Email service timeout"
                ));
        }

        // Simulate notification sent
        notificationsSent.increment();
        log.info("Notification sent successfully - order: {} product: {} amount: {}",
            orderId, product, amount);

        return ResponseEntity.ok(Map.of(
            "status", "SENT",
            "orderId", orderId,
            "customerId", customerId,
            "message", "Order confirmation sent for " + product + " worth ₹" + amount
        ));
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "notification-service");
    }
}
