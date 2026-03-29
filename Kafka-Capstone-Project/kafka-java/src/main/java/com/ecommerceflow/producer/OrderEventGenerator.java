package com.ecommerceflow.producer;

import com.ecommerceflow.model.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Order event generator for testing and demo
 * Generates sample order events with realistic data
 */
@Slf4j
@Component
public class OrderEventGenerator {

    private static final String[] CUSTOMERS = {
        "CUST_00001", "CUST_00002", "CUST_00003", "CUST_00004", "CUST_00005",
        "CUST_00006", "CUST_00007", "CUST_00008", "CUST_00009", "CUST_00010"
    };

    private static final String[] PRODUCTS = {
        "PROD_0001", "PROD_0002", "PROD_0003", "PROD_0004", "PROD_0005",
        "PROD_0006", "PROD_0007", "PROD_0008", "PROD_0009", "PROD_0010"
    };

    private static final String[] STATES = {
        "CA", "NY", "TX", "FL", "WA", "IL", "PA", "OH"
    };

    private static final double[] PRODUCT_PRICES = {
        25.99, 45.99, 99.99, 15.99, 150.00, 75.50, 200.00, 35.99, 80.00, 120.00
    };

    /**
     * Generate a sample order event
     */
    public OrderEvent generateOrderEvent(OrderEvent.EventType eventType) {
        Random random = new Random();
        String orderId = "ORD_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String customerId = CUSTOMERS[random.nextInt(CUSTOMERS.length)];
        
        // Generate product items
        int numItems = random.nextInt(5) + 1;
        List<OrderEvent.ProductItem> productItems = new ArrayList<>();
        double total = 0;
        
        for (int i = 0; i < numItems; i++) {
            int productIdx = random.nextInt(PRODUCTS.length);
            int quantity = random.nextInt(3) + 1;
            double unitPrice = PRODUCT_PRICES[productIdx];
            total += unitPrice * quantity;
            
            productItems.add(OrderEvent.ProductItem.builder()
                .productId(PRODUCTS[productIdx])
                .productName("Product " + (productIdx + 1))
                .quantity(quantity)
                .unitPrice(unitPrice)
                .build());
        }

        // Create shipping address
        OrderEvent.ShippingAddress address = OrderEvent.ShippingAddress.builder()
            .street((random.nextInt(999) + 1) + " Main St")
            .city("City_" + random.nextInt(100))
            .state(STATES[random.nextInt(STATES.length)])
            .zipCode(String.format("%05d", random.nextInt(100000)))
            .country("USA")
            .build();

        return OrderEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .orderId(orderId)
            .eventType(eventType)
            .customerId(customerId)
            .orderTotal(Math.round(total * 100.0) / 100.0)
            .productItems(productItems)
            .timestamp(System.currentTimeMillis())
            .status(eventType.toString().replace("_", " "))
            .shippingAddress(address)
            .build();
    }

    /**
     * Generate a sequence of events for an order lifecycle
     */
    public List<OrderEvent> generateOrderLifecycle() {
        OrderEvent.EventType[] lifecycle = {
            OrderEvent.EventType.ORDER_CREATED,
            OrderEvent.EventType.PAYMENT_PROCESSED,
            OrderEvent.EventType.ORDER_SHIPPED,
            OrderEvent.EventType.ORDER_DELIVERED
        };

        // Generate all events for same order
        List<OrderEvent> events = new ArrayList<>();
        String orderId = "ORD_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        for (OrderEvent.EventType eventType : lifecycle) {
            OrderEvent event = generateOrderEvent(eventType);
            event.setOrderId(orderId); // Same order ID for lifecycle
            events.add(event);
        }
        
        return events;
    }
}
