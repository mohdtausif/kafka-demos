package com.ecommerceflow.analytics.listener;

import com.ecommerceflow.avro.OrderCreated;
import com.ecommerceflow.avro.OrderDelivered;
import com.ecommerceflow.avro.OrderPaid;
import com.ecommerceflow.avro.OrderShipped;
import com.ecommerceflow.analytics.service.AnalyticsService;
import com.ecommerceflow.common.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAnalyticsListener {

    private final AnalyticsService analyticsService;

    @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = "analytics-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onOrderCreated(ConsumerRecord<String, OrderCreated> record, Acknowledgment ack) {
        try {
            OrderCreated event = record.value();
            log.info("[ANALYTICS] Order CREATED: orderId={}, customerId={}, productId={}, partition={}, offset={}",
                    event.getOrderId(), event.getCustomerId(), event.getProductId(),
                    record.partition(), record.offset());

            analyticsService.recordOrderCreated(event.getOrderId().toString(), event.getProductId().toString());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[ANALYTICS] Error processing OrderCreated: {}", e.getMessage(), e);
            throw e; // Let the error handler deal with it (DLQ)
        }
    }

    @KafkaListener(topics = KafkaTopics.ORDER_PAID, groupId = "analytics-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onOrderPaid(ConsumerRecord<String, OrderPaid> record, Acknowledgment ack) {
        try {
            OrderPaid event = record.value();
            BigDecimal amount = bytesToDecimal(event.getTotalAmount());
            log.info("[ANALYTICS] Order PAID: orderId={}, amount={}, paymentMethod={}, partition={}, offset={}",
                    event.getOrderId(), amount, event.getPaymentMethod(),
                    record.partition(), record.offset());

            analyticsService.recordOrderPaid(event.getOrderId().toString(), amount);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[ANALYTICS] Error processing OrderPaid: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = KafkaTopics.ORDER_SHIPPED, groupId = "analytics-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onOrderShipped(ConsumerRecord<String, OrderShipped> record, Acknowledgment ack) {
        try {
            OrderShipped event = record.value();
            log.info("[ANALYTICS] Order SHIPPED: orderId={}, carrier={}, trackingNumber={}, partition={}, offset={}",
                    event.getOrderId(), event.getCarrier(), event.getTrackingNumber(),
                    record.partition(), record.offset());

            analyticsService.recordOrderShipped(event.getOrderId().toString());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[ANALYTICS] Error processing OrderShipped: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = KafkaTopics.ORDER_DELIVERED, groupId = "analytics-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onOrderDelivered(ConsumerRecord<String, OrderDelivered> record, Acknowledgment ack) {
        try {
            OrderDelivered event = record.value();
            log.info("[ANALYTICS] Order DELIVERED: orderId={}, deliveredTo={}, partition={}, offset={}",
                    event.getOrderId(), event.getDeliveredTo(),
                    record.partition(), record.offset());

            analyticsService.recordOrderDelivered(event.getOrderId().toString());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[ANALYTICS] Error processing OrderDelivered: {}", e.getMessage(), e);
            throw e;
        }
    }

    private BigDecimal bytesToDecimal(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new BigDecimal(new BigInteger(bytes), 2);
    }
}
