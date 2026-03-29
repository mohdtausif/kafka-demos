package com.ecommerceflow.controller;

import com.ecommerceflow.consumer.AnalyticsConsumerService;
import com.ecommerceflow.consumer.InventoryConsumerService;
import com.ecommerceflow.consumer.NotificationConsumerService;
import com.ecommerceflow.model.OrderEvent;
import com.ecommerceflow.producer.OrderEventGenerator;
import com.ecommerceflow.producer.OrderProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST Controller for Order Event Producer and Metrics
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class OrderEventController {

    @Autowired
    private OrderProducerService producerService;

    @Autowired
    private OrderEventGenerator eventGenerator;

    @Autowired
    private AnalyticsConsumerService analyticsConsumerService;

    @Autowired
    private NotificationConsumerService notificationConsumerService;

    @Autowired
    private InventoryConsumerService inventoryConsumerService;

    /**
     * Send a single order event
     */
    @PostMapping("/producer/send")
    public ResponseEntity<?> sendOrderEvent(@RequestParam(defaultValue = "ORDER_CREATED") String eventType) {
        try {
            OrderEvent.EventType type = OrderEvent.EventType.valueOf(eventType);
            OrderEvent event = eventGenerator.generateOrderEvent(type);
            producerService.sendOrderEvent(event);
            
            return ResponseEntity.ok(Map.of(
                "status", "sent",
                "orderId", event.getOrderId(),
                "eventType", event.getEventType().toString(),
                "message", "Order event sent successfully"
            ));
        } catch (Exception e) {
            log.error("Error sending order event", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Send batch of order events
     */
    @PostMapping("/producer/send-batch")
    public ResponseEntity<?> sendOrderEventBatch(@RequestParam(defaultValue = "100") int count) {
        try {
            List<OrderEvent> events = new ArrayList<>();
            Random random = new Random();
            OrderEvent.EventType[] eventTypes = OrderEvent.EventType.values();
            
            for (int i = 0; i < count; i++) {
                OrderEvent.EventType type = eventTypes[random.nextInt(eventTypes.length)];
                events.add(eventGenerator.generateOrderEvent(type));
            }
            
            producerService.sendOrderEventsBatch(events);
            
            return ResponseEntity.ok(Map.of(
                "status", "sent",
                "count", count,
                "message", count + " order events sent successfully"
            ));
        } catch (Exception e) {
            log.error("Error sending batch of order events", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Send complete order lifecycle
     */
    @PostMapping("/producer/send-lifecycle")
    public ResponseEntity<?> sendOrderLifecycle() {
        try {
            List<OrderEvent> lifecycle = eventGenerator.generateOrderLifecycle();
            producerService.sendOrderEventsBatch(lifecycle);
            
            return ResponseEntity.ok(Map.of(
                "status", "sent",
                "orderId", lifecycle.get(0).getOrderId(),
                "events", 4,
                "message", "Complete order lifecycle sent successfully"
            ));
        } catch (Exception e) {
            log.error("Error sending order lifecycle", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get analytics metrics
     */
    @GetMapping("/metrics/analytics")
    public ResponseEntity<?> getAnalyticsMetrics() {
        try {
            return ResponseEntity.ok(analyticsConsumerService.getMetrics());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get inventory status
     */
    @GetMapping("/metrics/inventory")
    public ResponseEntity<?> getInventoryStatus() {
        try {
            return ResponseEntity.ok(Map.of(
                "inventory", inventoryConsumerService.getInventoryStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get specific product inventory
     */
    @GetMapping("/metrics/inventory/{productId}")
    public ResponseEntity<?> getProductInventory(@PathVariable String productId) {
        try {
            return ResponseEntity.ok(inventoryConsumerService.getProductInventory(productId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get notification statistics
     */
    @GetMapping("/metrics/notifications")
    public ResponseEntity<?> getNotificationStats() {
        try {
            return ResponseEntity.ok(Map.of(
                "stats", notificationConsumerService.getStats(),
                "dlqMessages", notificationConsumerService.getDLQMessages()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "application", "kafka-capstone",
            "timestamp", System.currentTimeMillis()
        ));
    }
}
