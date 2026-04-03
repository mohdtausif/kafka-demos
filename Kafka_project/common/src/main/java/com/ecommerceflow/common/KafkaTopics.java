package com.ecommerceflow.common;

/**
 * Centralized Kafka topic name constants for all services.
 */
public final class KafkaTopics {

    private KafkaTopics() {
        // Utility class
    }

    // Order lifecycle topics
    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_PAID = "order.paid";
    public static final String ORDER_SHIPPED = "order.shipped";
    public static final String ORDER_DELIVERED = "order.delivered";

    // Dead letter queue
    public static final String ORDER_DLQ = "order.dlq";

    // Analytics output topics
    public static final String ANALYTICS_ORDER_COUNTS = "order.analytics.counts";
    public static final String ANALYTICS_REVENUE = "order.analytics.revenue";

    // Connector topics
    public static final String CUSTOMERS = "customers";
    public static final String NOTIFICATIONS = "notifications";
}
