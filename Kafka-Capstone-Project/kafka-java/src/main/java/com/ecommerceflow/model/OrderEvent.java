package com.ecommerceflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Order Event domain model
 * Represents order lifecycle events for Kafka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent {
    
    private String eventId;
    private String orderId;
    private EventType eventType;
    private String customerId;
    private Double orderTotal;
    private List<ProductItem> productItems;
    private Long timestamp;
    private String status;
    private ShippingAddress shippingAddress;

    public enum EventType {
        ORDER_CREATED,
        PAYMENT_PROCESSED,
        ORDER_SHIPPED,
        ORDER_DELIVERED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductItem {
        private String productId;
        private String productName;
        private Integer quantity;
        private Double unitPrice;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShippingAddress {
        private String street;
        private String city;
        private String state;
        private String zipCode;
        private String country;
    }
}
