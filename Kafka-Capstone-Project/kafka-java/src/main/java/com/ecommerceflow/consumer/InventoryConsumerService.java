package com.ecommerceflow.consumer;

import com.ecommerceflow.model.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Inventory Consumer Service
 * Processes order events for inventory management
 * Updates inventory based on order shipments
 */
@Slf4j
@Service
public class InventoryConsumerService {

    private final AtomicLong processedCount = new AtomicLong(0);
    
    // In-memory inventory store (in production: use database)
    private final Map<String, InventoryItem> inventory = new ConcurrentHashMap<>();

    public InventoryConsumerService() {
        // Initialize sample inventory
        for (int i = 1; i <= 10; i++) {
            String productId = String.format("PROD_%04d", i);
            inventory.put(productId, new InventoryItem(productId, 1000, 0));
        }
    }

    /**
     * Consume order events and update inventory
     */
    @KafkaListener(
        topics = "${app.consumer.inventory.topic}",
        groupId = "${app.consumer.inventory.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void processOrderEvent(
        @Payload OrderEvent orderEvent,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment) {

        try {
            updateInventory(orderEvent);
            
            processedCount.incrementAndGet();
            long count = processedCount.get();

            if (count % 10 == 0) {
                logInventoryStatus();
            }

            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error updating inventory for order: {}", orderEvent.getOrderId(), e);
            acknowledgment.acknowledge();
        }
    }

    /**
     * Update inventory based on event type
     */
    private void updateInventory(OrderEvent event) {
        switch (event.getEventType()) {
            case ORDER_CREATED:
                // Reserve inventory
                for (OrderEvent.ProductItem item : event.getProductItems()) {
                    String productId = item.getProductId();
                    int quantity = item.getQuantity();
                    
                    InventoryItem inv = inventory.getOrDefault(productId, 
                        new InventoryItem(productId, 1000, 0));
                    inv.setReserved(inv.getReserved() + quantity);
                    inventory.put(productId, inv);
                    
                    log.info("[INVENTORY] Reserved {} units of {}", quantity, productId);
                }
                break;

            case ORDER_SHIPPED:
                // Deduct from stock
                for (OrderEvent.ProductItem item : event.getProductItems()) {
                    String productId = item.getProductId();
                    int quantity = item.getQuantity();
                    
                    InventoryItem inv = inventory.getOrDefault(productId, 
                        new InventoryItem(productId, 1000, 0));
                    inv.setStock(inv.getStock() - quantity);
                    inv.setReserved(Math.max(0, inv.getReserved() - quantity));
                    inventory.put(productId, inv);
                    
                    log.info("[INVENTORY] Deducted {} units of {}, remaining: {}", 
                        quantity, productId, inv.getStock());
                }
                break;

            default:
                // Other event types don't affect inventory
                break;
        }
    }

    /**
     * Log inventory status
     */
    private void logInventoryStatus() {
        log.info("\n=== Inventory Status (processed {} events) ===", processedCount.get());
        
        List<String> lowStockProducts = new ArrayList<>();
        inventory.forEach((productId, item) -> {
            if (item.getStock() < 100) {
                lowStockProducts.add(productId);
            }
        });

        if (!lowStockProducts.isEmpty()) {
            log.warn("⚠️  Low stock products: {}", lowStockProducts);
        } else {
            log.info("✓ All products adequately stocked");
        }
    }

    /**
     * Get current inventory status
     */
    public Map<String, InventoryItem> getInventoryStatus() {
        return new HashMap<>(inventory);
    }

    /**
     * Get inventory for specific product
     */
    public InventoryItem getProductInventory(String productId) {
        return inventory.getOrDefault(productId, 
            new InventoryItem(productId, 0, 0));
    }

    /**
     * Inventory item data class
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class InventoryItem {
        private String productId;
        private Integer stock;
        private Integer reserved;

        public Integer getAvailable() {
            return stock - reserved;
        }
    }
}
