package com.ecommerceflow.consumer;

import com.ecommerceflow.model.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Notification Consumer Service
 * Processes order events to send notifications
 * Implements Dead Letter Queue support for failed notifications
 */
@Slf4j
@Service
public class NotificationConsumerService {

    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong dlqCount = new AtomicLong(0);
    private final List<FailedNotification> dlqMessages = new ArrayList<>();

    /**
     * Consume order events and send notifications
     */
    @KafkaListener(
        topics = "${app.consumer.notification.topic}",
        groupId = "${app.consumer.notification.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void processOrderEvent(
        @Payload OrderEvent orderEvent,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment) {

        try {
            // Send notification
            if (sendNotification(orderEvent)) {
                processedCount.incrementAndGet();
                acknowledgment.acknowledge();
                
                long count = processedCount.get();
                if (count % 20 == 0) {
                    log.info("✓ Processed {} notifications (DLQ: {})", count, dlqCount.get());
                }
            } else {
                handleFailedNotification(orderEvent);
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            log.error("Error processing notification for order: {}", orderEvent.getOrderId(), e);
            handleFailedNotification(orderEvent);
            acknowledgment.acknowledge();
        }
    }

    /**
     * Send notification to customer
     */
    private boolean sendNotification(OrderEvent event) {
        String customerId = event.getCustomerId();
        String eventType = event.getEventType().toString();
        String orderId = event.getOrderId();

        String message = switch (eventType) {
            case "ORDER_CREATED" -> String.format("Order %s has been placed successfully", orderId);
            case "PAYMENT_PROCESSED" -> String.format("Payment confirmed for order %s", orderId);
            case "ORDER_SHIPPED" -> String.format("Order %s has been shipped", orderId);
            case "ORDER_DELIVERED" -> String.format("Order %s has been delivered", orderId);
            default -> "Order update";
        };

        log.info("[NOTIFICATION] {}: {}", customerId, message);
        return true; // Simulate notification success
    }

    /**
     * Handle failed notification - send to DLQ
     */
    private void handleFailedNotification(OrderEvent orderEvent) {
        dlqCount.incrementAndGet();
        dlqMessages.add(FailedNotification.builder()
            .timestamp(System.currentTimeMillis())
            .orderId(orderEvent.getOrderId())
            .customerId(orderEvent.getCustomerId())
            .reason("Notification delivery failed")
            .build());
        
        log.warn("[DLQ] Failed to send notification for order: {}", orderEvent.getOrderId());
    }

    /**
     * Get DLQ messages
     */
    public List<FailedNotification> getDLQMessages() {
        return new ArrayList<>(dlqMessages);
    }

    /**
     * Get statistics
     */
    public NotificationStats getStats() {
        return NotificationStats.builder()
            .processedCount(processedCount.get())
            .dlqCount(dlqCount.get())
            .dlqMessages(dlqMessages.size())
            .build();
    }

    /**
     * Data class for failed notifications
     */
    @lombok.Data
    @lombok.Builder
    public static class FailedNotification {
        private Long timestamp;
        private String orderId;
        private String customerId;
        private String reason;
    }

    /**
     * Data class for notification statistics
     */
    @lombok.Data
    @lombok.Builder
    public static class NotificationStats {
        private Long processedCount;
        private Long dlqCount;
        private Integer dlqMessages;
    }
}
