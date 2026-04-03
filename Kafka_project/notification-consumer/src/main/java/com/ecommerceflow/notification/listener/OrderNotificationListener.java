package com.ecommerceflow.notification.listener;

import com.ecommerceflow.avro.OrderCreated;
import com.ecommerceflow.avro.OrderDelivered;
import com.ecommerceflow.avro.OrderPaid;
import com.ecommerceflow.avro.OrderShipped;
import com.ecommerceflow.common.KafkaTopics;
import com.ecommerceflow.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = "notification-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onOrderCreated(ConsumerRecord<String, OrderCreated> record, Acknowledgment ack) {
        try {
            OrderCreated event = record.value();
            log.info("[NOTIFICATION] Received ORDER_CREATED: orderId={}, partition={}, offset={}",
                    event.getOrderId(), record.partition(), record.offset());

            notificationService.sendOrderConfirmation(
                    event.getOrderId().toString(),
                    event.getCustomerId().toString(),
                    event.getProductId().toString());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[NOTIFICATION] Error processing OrderCreated: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = KafkaTopics.ORDER_PAID, groupId = "notification-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onOrderPaid(ConsumerRecord<String, OrderPaid> record, Acknowledgment ack) {
        try {
            OrderPaid event = record.value();
            log.info("[NOTIFICATION] Received ORDER_PAID: orderId={}, partition={}, offset={}",
                    event.getOrderId(), record.partition(), record.offset());

            notificationService.sendPaymentConfirmation(
                    event.getOrderId().toString(),
                    event.getCustomerId().toString(),
                    event.getPaymentMethod().toString());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[NOTIFICATION] Error processing OrderPaid: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = KafkaTopics.ORDER_SHIPPED, groupId = "notification-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onOrderShipped(ConsumerRecord<String, OrderShipped> record, Acknowledgment ack) {
        try {
            OrderShipped event = record.value();
            log.info("[NOTIFICATION] Received ORDER_SHIPPED: orderId={}, partition={}, offset={}",
                    event.getOrderId(), record.partition(), record.offset());

            notificationService.sendShipmentNotification(
                    event.getOrderId().toString(),
                    event.getCustomerId().toString(),
                    event.getTrackingNumber().toString(),
                    event.getCarrier().toString());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[NOTIFICATION] Error processing OrderShipped: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = KafkaTopics.ORDER_DELIVERED, groupId = "notification-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onOrderDelivered(ConsumerRecord<String, OrderDelivered> record, Acknowledgment ack) {
        try {
            OrderDelivered event = record.value();
            log.info("[NOTIFICATION] Received ORDER_DELIVERED: orderId={}, partition={}, offset={}",
                    event.getOrderId(), record.partition(), record.offset());

            notificationService.sendDeliveryConfirmation(
                    event.getOrderId().toString(),
                    event.getCustomerId().toString(),
                    event.getDeliveredTo().toString());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[NOTIFICATION] Error processing OrderDelivered: {}", e.getMessage(), e);
            throw e;
        }
    }
}
