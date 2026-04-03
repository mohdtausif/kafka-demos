package com.ecommerceflow.producer.service;

import com.ecommerceflow.avro.OrderCreated;
import com.ecommerceflow.avro.OrderDelivered;
import com.ecommerceflow.avro.OrderPaid;
import com.ecommerceflow.avro.OrderShipped;
import com.ecommerceflow.common.KafkaTopics;
import com.ecommerceflow.producer.model.DeliveryRequest;
import com.ecommerceflow.producer.model.OrderRequest;
import com.ecommerceflow.producer.model.PaymentRequest;
import com.ecommerceflow.producer.model.ShipmentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;

    /**
     * Publishes an OrderCreated event when a new order is placed.
     * Uses orderId as the partition key to ensure ordering per order.
     */
    public String publishOrderCreated(OrderRequest request) {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        OrderCreated event = OrderCreated.newBuilder()
                .setOrderId(orderId)
                .setCustomerId(request.getCustomerId())
                .setProductId(request.getProductId())
                .setProductName(request.getProductName())
                .setQuantity(request.getQuantity())
                .setUnitPrice(decimalToBytes(request.getPrice()))
                .setTotalAmount(decimalToBytes(request.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()))))
                .setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .setShippingAddress(request.getShippingAddress())
                .setStatus("CREATED")
                .setCreatedAt(Instant.now())
                .setMetadata(null)
                .build();

        sendEvent(KafkaTopics.ORDER_CREATED, orderId, event);
        log.info("Published ORDER_CREATED event for orderId={}, customerId={}, productId={}",
                orderId, request.getCustomerId(), request.getProductId());

        return orderId;
    }

    /**
     * Publishes an OrderPaid event when payment is processed.
     */
    public void publishOrderPaid(String orderId, String customerId, String productId,
                                  BigDecimal totalAmount, PaymentRequest payment) {
        OrderPaid event = OrderPaid.newBuilder()
                .setOrderId(orderId)
                .setCustomerId(customerId)
                .setProductId(productId)
                .setTotalAmount(decimalToBytes(totalAmount))
                .setCurrency("USD")
                .setPaymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "CREDIT_CARD")
                .setTransactionId(payment.getTransactionId() != null ? payment.getTransactionId() : "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .setStatus("PAID")
                .setPaidAt(Instant.now())
                .setMetadata(null)
                .build();

        sendEvent(KafkaTopics.ORDER_PAID, orderId, event);
        log.info("Published ORDER_PAID event for orderId={}", orderId);
    }

    /**
     * Publishes an OrderShipped event when the order is shipped.
     */
    public void publishOrderShipped(String orderId, String customerId, String productId,
                                     int quantity, ShipmentRequest shipment) {
        OrderShipped event = OrderShipped.newBuilder()
                .setOrderId(orderId)
                .setCustomerId(customerId)
                .setProductId(productId)
                .setQuantity(quantity)
                .setTrackingNumber(shipment.getTrackingNumber() != null ? shipment.getTrackingNumber() : "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .setCarrier(shipment.getCarrier() != null ? shipment.getCarrier() : "FEDEX")
                .setShippingAddress(shipment.getShippingAddress() != null ? shipment.getShippingAddress() : "Default Address")
                .setEstimatedDelivery(shipment.getEstimatedDelivery())
                .setStatus("SHIPPED")
                .setShippedAt(Instant.now())
                .setMetadata(null)
                .build();

        sendEvent(KafkaTopics.ORDER_SHIPPED, orderId, event);
        log.info("Published ORDER_SHIPPED event for orderId={}", orderId);
    }

    /**
     * Publishes an OrderDelivered event when the order is delivered.
     */
    public void publishOrderDelivered(String orderId, String customerId, String productId,
                                       int quantity, DeliveryRequest delivery) {
        OrderDelivered event = OrderDelivered.newBuilder()
                .setOrderId(orderId)
                .setCustomerId(customerId)
                .setProductId(productId)
                .setQuantity(quantity)
                .setDeliveredTo(delivery.getDeliveredTo() != null ? delivery.getDeliveredTo() : "Customer")
                .setSignatureRequired(delivery.isSignatureRequired())
                .setDeliveryNotes(delivery.getDeliveryNotes())
                .setStatus("DELIVERED")
                .setDeliveredAt(Instant.now())
                .setMetadata(null)
                .build();

        sendEvent(KafkaTopics.ORDER_DELIVERED, orderId, event);
        log.info("Published ORDER_DELIVERED event for orderId={}", orderId);
    }

    /**
     * Sends an event to the specified Kafka topic with the orderId as the key
     * for partition-based ordering guarantees.
     */
    private void sendEvent(String topic, String key, SpecificRecord event) {
        CompletableFuture<SendResult<String, SpecificRecord>> future =
                kafkaTemplate.send(new ProducerRecord<>(topic, key, event));

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send event to topic={}, key={}: {}", topic, key, ex.getMessage(), ex);
            } else {
                RecordMetadata metadata = result.getRecordMetadata();
                log.debug("Event sent successfully: topic={}, partition={}, offset={}, key={}",
                        metadata.topic(), metadata.partition(), metadata.offset(), key);
            }
        });
    }

    /**
     * Converts BigDecimal to ByteBuffer for Avro decimal logical type.
     */
    private ByteBuffer decimalToBytes(BigDecimal value) {
        return ByteBuffer.wrap(value.unscaledValue().toByteArray());
    }
}
