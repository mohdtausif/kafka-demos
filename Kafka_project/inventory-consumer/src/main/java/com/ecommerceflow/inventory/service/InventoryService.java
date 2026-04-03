package com.ecommerceflow.inventory.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing product inventory.
 * Tracks available and reserved quantities per product.
 */
@Slf4j
@Service
public class InventoryService {

    // productId -> available quantity
    private final Map<String, Integer> availableStock = new ConcurrentHashMap<>();

    // productId -> reserved quantity
    private final Map<String, Integer> reservedStock = new ConcurrentHashMap<>();

    // Initialize with default stock
    {
        availableStock.put("P001", 500);
        availableStock.put("P002", 1000);
        availableStock.put("P003", 750);
        availableStock.put("P004", 300);
        availableStock.put("P005", 200);
        availableStock.put("P006", 400);
        availableStock.put("P007", 600);
        availableStock.put("P008", 350);
        availableStock.put("P009", 250);
        availableStock.put("P010", 450);
    }

    /**
     * Reserve stock when an order is created.
     * Moves quantity from available to reserved.
     */
    public boolean reserveStock(String productId, int quantity) {
        return availableStock.compute(productId, (key, available) -> {
            if (available == null || available < quantity) {
                log.warn("⚠️ Insufficient stock for product {}: available={}, requested={}",
                        productId, available, quantity);
                return available;
            }
            reservedStock.merge(productId, quantity, Integer::sum);
            log.info("📋 Stock RESERVED: product={}, reserved={}, remaining={}",
                    productId, quantity, available - quantity);
            return available - quantity;
        }) != null;
    }

    /**
     * Deduct stock when an order is shipped.
     * Removes quantity from reserved.
     */
    public void deductStock(String productId, int quantity) {
        reservedStock.computeIfPresent(productId, (key, reserved) -> {
            int newReserved = Math.max(0, reserved - quantity);
            log.info("📦 Stock DEDUCTED: product={}, deducted={}, remainingReserved={}",
                    productId, quantity, newReserved);
            return newReserved == 0 ? null : newReserved;
        });
    }

    /**
     * Release reserved stock (e.g., when order is cancelled).
     */
    public void releaseStock(String productId, int quantity) {
        reservedStock.computeIfPresent(productId, (key, reserved) -> {
            int release = Math.min(reserved, quantity);
            availableStock.merge(productId, release, Integer::sum);
            log.info("🔄 Stock RELEASED: product={}, released={}", productId, release);
            return reserved - release <= 0 ? null : reserved - release;
        });
    }

    public Map<String, Object> getInventoryStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("availableStock", new ConcurrentHashMap<>(availableStock));
        status.put("reservedStock", new ConcurrentHashMap<>(reservedStock));
        return status;
    }
}
