package com.ecommerceflow.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service that simulates sending notifications (email, SMS, push).
 * In production, this would integrate with real notification providers.
 */
@Slf4j
@Service
public class NotificationService {

    private final AtomicLong notificationCounter = new AtomicLong(0);
    private final List<Map<String, String>> notificationLog = new ArrayList<>();

    public void sendOrderConfirmation(String orderId, String customerId, String productId) {
        long notifId = notificationCounter.incrementAndGet();
        log.info("📧 [NOTIFICATION #{}] ORDER CONFIRMATION sent to customer {} for order {} (product: {})",
                notifId, customerId, orderId, productId);

        logNotification("ORDER_CONFIRMATION", orderId, customerId,
                String.format("Your order %s for product %s has been placed successfully!", orderId, productId));
    }

    public void sendPaymentConfirmation(String orderId, String customerId, String paymentMethod) {
        long notifId = notificationCounter.incrementAndGet();
        log.info("💳 [NOTIFICATION #{}] PAYMENT CONFIRMATION sent to customer {} for order {} via {}",
                notifId, customerId, orderId, paymentMethod);

        logNotification("PAYMENT_CONFIRMATION", orderId, customerId,
                String.format("Payment for order %s has been processed via %s.", orderId, paymentMethod));
    }

    public void sendShipmentNotification(String orderId, String customerId, String trackingNumber, String carrier) {
        long notifId = notificationCounter.incrementAndGet();
        log.info("📦 [NOTIFICATION #{}] SHIPMENT UPDATE sent to customer {} for order {} - tracking: {} via {}",
                notifId, customerId, orderId, trackingNumber, carrier);

        logNotification("SHIPMENT_UPDATE", orderId, customerId,
                String.format("Your order %s has been shipped via %s. Tracking: %s", orderId, carrier, trackingNumber));
    }

    public void sendDeliveryConfirmation(String orderId, String customerId, String deliveredTo) {
        long notifId = notificationCounter.incrementAndGet();
        log.info("✅ [NOTIFICATION #{}] DELIVERY CONFIRMATION sent to customer {} for order {} - delivered to: {}",
                notifId, customerId, orderId, deliveredTo);

        logNotification("DELIVERY_CONFIRMATION", orderId, customerId,
                String.format("Your order %s has been delivered to %s.", orderId, deliveredTo));
    }

    private void logNotification(String type, String orderId, String customerId, String message) {
        Map<String, String> entry = new ConcurrentHashMap<>();
        entry.put("type", type);
        entry.put("orderId", orderId);
        entry.put("customerId", customerId);
        entry.put("message", message);
        entry.put("timestamp", java.time.Instant.now().toString());
        notificationLog.add(entry);
    }

    public List<Map<String, String>> getNotificationLog() {
        return new ArrayList<>(notificationLog);
    }

    public long getNotificationCount() {
        return notificationCounter.get();
    }
}
