package com.ecommerceflow.inventory.listener;

import com.ecommerceflow.avro.OrderCreated;
import com.ecommerceflow.avro.OrderShipped;
import com.ecommerceflow.common.KafkaTopics;
import com.ecommerceflow.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderInventoryListener {

    private final InventoryService inventoryService;

    /**
     * When an order is created, reserve stock for the ordered product.
     */
    @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = "inventory-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onOrderCreated(ConsumerRecord<String, OrderCreated> record, Acknowledgment ack) {
        try {
            OrderCreated event = record.value();
            log.info("[INVENTORY] Received ORDER_CREATED: orderId={}, productId={}, qty={}, partition={}, offset={}",
                    event.getOrderId(), event.getProductId(), event.getQuantity(),
                    record.partition(), record.offset());

            boolean reserved = inventoryService.reserveStock(
                    event.getProductId().toString(),
                    event.getQuantity());

            if (reserved) {
                log.info("[INVENTORY] Stock reserved for order {}", event.getOrderId());
            } else {
                log.warn("[INVENTORY] Failed to reserve stock for order {}", event.getOrderId());
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("[INVENTORY] Error processing OrderCreated: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * When an order is shipped, deduct the reserved stock.
     */
    @KafkaListener(topics = KafkaTopics.ORDER_SHIPPED, groupId = "inventory-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onOrderShipped(ConsumerRecord<String, OrderShipped> record, Acknowledgment ack) {
        try {
            OrderShipped event = record.value();
            log.info("[INVENTORY] Received ORDER_SHIPPED: orderId={}, productId={}, qty={}, partition={}, offset={}",
                    event.getOrderId(), event.getProductId(), event.getQuantity(),
                    record.partition(), record.offset());

            inventoryService.deductStock(
                    event.getProductId().toString(),
                    event.getQuantity());

            log.info("[INVENTORY] Stock deducted for shipped order {}", event.getOrderId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[INVENTORY] Error processing OrderShipped: {}", e.getMessage(), e);
            throw e;
        }
    }
}
