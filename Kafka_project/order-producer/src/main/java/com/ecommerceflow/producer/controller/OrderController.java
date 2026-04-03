package com.ecommerceflow.producer.controller;

import com.ecommerceflow.producer.model.DeliveryRequest;
import com.ecommerceflow.producer.model.OrderRequest;
import com.ecommerceflow.producer.model.PaymentRequest;
import com.ecommerceflow.producer.model.ShipmentRequest;
import com.ecommerceflow.producer.service.OrderEventProducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderEventProducer orderEventProducer;

    // In-memory order store for demo purposes (use DB in production)
    private final Map<String, OrderRequest> orderStore = new ConcurrentHashMap<>();

    /**
     * POST /api/orders - Create a new order
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@Valid @RequestBody OrderRequest request) {
        log.info("Received create order request: customerId={}, productId={}, qty={}",
                request.getCustomerId(), request.getProductId(), request.getQuantity());

        String orderId = orderEventProducer.publishOrderCreated(request);
        orderStore.put(orderId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "orderId", orderId,
                "status", "CREATED",
                "message", "Order created successfully"
        ));
    }

    /**
     * PUT /api/orders/{orderId}/pay - Process payment for an order
     */
    @PutMapping("/{orderId}/pay")
    public ResponseEntity<Map<String, Object>> payOrder(
            @PathVariable("orderId") String orderId,
            @RequestBody(required = false) PaymentRequest payment) {

        OrderRequest order = orderStore.get(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (payment == null) {
            payment = PaymentRequest.builder()
                    .paymentMethod("CREDIT_CARD")
                    .build();
        }

        BigDecimal totalAmount = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
        orderEventProducer.publishOrderPaid(orderId, order.getCustomerId(), order.getProductId(),
                totalAmount, payment);

        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "status", "PAID",
                "message", "Payment processed successfully"
        ));
    }

    /**
     * PUT /api/orders/{orderId}/ship - Ship the order
     */
    @PutMapping("/{orderId}/ship")
    public ResponseEntity<Map<String, Object>> shipOrder(
            @PathVariable("orderId") String orderId,
            @RequestBody(required = false) ShipmentRequest shipment) {

        OrderRequest order = orderStore.get(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (shipment == null) {
            shipment = ShipmentRequest.builder()
                    .carrier("FEDEX")
                    .shippingAddress(order.getShippingAddress())
                    .build();
        }

        orderEventProducer.publishOrderShipped(orderId, order.getCustomerId(), order.getProductId(),
                order.getQuantity(), shipment);

        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "status", "SHIPPED",
                "message", "Order shipped successfully"
        ));
    }

    /**
     * PUT /api/orders/{orderId}/deliver - Mark order as delivered
     */
    @PutMapping("/{orderId}/deliver")
    public ResponseEntity<Map<String, Object>> deliverOrder(
            @PathVariable("orderId") String orderId,
            @RequestBody(required = false) DeliveryRequest delivery) {

        OrderRequest order = orderStore.get(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (delivery == null) {
            delivery = DeliveryRequest.builder()
                    .deliveredTo("Customer")
                    .signatureRequired(false)
                    .build();
        }

        orderEventProducer.publishOrderDelivered(orderId, order.getCustomerId(), order.getProductId(),
                order.getQuantity(), delivery);

        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "status", "DELIVERED",
                "message", "Order delivered successfully"
        ));
    }

    /**
     * GET /api/orders - List all orders in memory
     */
    @GetMapping
    public ResponseEntity<Map<String, OrderRequest>> listOrders() {
        return ResponseEntity.ok(orderStore);
    }
}
