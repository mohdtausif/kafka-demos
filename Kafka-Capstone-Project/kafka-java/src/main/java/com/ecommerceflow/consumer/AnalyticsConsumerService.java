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
 * Analytics Consumer Service
 * Processes order events for real-time metrics
 * Counts orders and aggregates revenue per time window
 */
@Slf4j
@Service
public class AnalyticsConsumerService {

    private final AtomicLong totalOrders = new AtomicLong(0);
    private final AtomicLong totalRevenue = new AtomicLong(0);
    private final Map<String, Long> ordersByType = new ConcurrentHashMap<>();
    private final Map<String, Long> ordersByCustomer = new ConcurrentHashMap<>();
    private final Map<String, Double> revenueByCustomer = new ConcurrentHashMap<>();

    /**
     * Consume order events and update analytics
     */
    @KafkaListener(
        topics = "${app.consumer.analytics.topic}",
        groupId = "${app.consumer.analytics.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void processOrderEvent(
        @Payload OrderEvent orderEvent,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment) {

        try {
            // Update analytics
            totalOrders.incrementAndGet();
            totalRevenue.addAndGet((long) (orderEvent.getOrderTotal() * 100));
            
            String eventType = orderEvent.getEventType().toString();
            ordersByType.merge(eventType, 1L, Long::sum);
            
            String customerId = orderEvent.getCustomerId();
            ordersByCustomer.merge(customerId, 1L, Long::sum);
            revenueByCustomer.merge(customerId, orderEvent.getOrderTotal(), Double::sum);

            long processedCount = totalOrders.get();
            
            // Log metrics every 10 messages
            if (processedCount % 10 == 0) {
                logAnalytics();
            }

            // Manual offset commit for exactly-once semantics
            acknowledgment.acknowledge();
            
            log.debug("Processed order event: orderId={}, type={}, partition={}, offset={}", 
                orderEvent.getOrderId(), eventType, partition, offset);

        } catch (Exception e) {
            log.error("Error processing order event: {}", orderEvent.getOrderId(), e);
            acknowledgment.acknowledge(); // Still commit to avoid reprocessing
        }
    }

    /**
     * Log current analytics metrics
     */
    private void logAnalytics() {
        log.info("\n=== Analytics Update (processed {} events) ===", totalOrders.get());
        log.info("Total Orders: {}", totalOrders.get());
        log.info("Total Revenue: ${}", String.format("%.2f", totalRevenue.get() / 100.0));
        log.info("Orders by Type: {}", ordersByType);
        
        // Top 5 customers by orders
        revenueByCustomer.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(5)
            .forEach(e -> log.info("  Top Customer: {} = ${}", e.getKey(), String.format("%.2f", e.getValue())));
    }

    /**
     * Get current metrics summary
     */
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalOrders", totalOrders.get());
        metrics.put("totalRevenue", totalRevenue.get() / 100.0);
        metrics.put("ordersByType", new HashMap<>(ordersByType));
        metrics.put("ordersByCustomer", new HashMap<>(ordersByCustomer));
        return metrics;
    }
}
