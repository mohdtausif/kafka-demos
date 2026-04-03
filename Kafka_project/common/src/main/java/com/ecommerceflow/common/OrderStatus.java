package com.ecommerceflow.common;

/**
 * Order status enum representing the lifecycle stages of an order.
 */
public enum OrderStatus {
    CREATED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    FAILED
}
