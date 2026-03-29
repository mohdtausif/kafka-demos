package com.ecommerceflow.producer;

import com.ecommerceflow.model.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for producing order events to Kafka
 * Implements reliable producer with proper configurations
 */
@Slf4j
@Service
public class OrderProducerService {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.producer.topic}")
    private String orderEventsTopic;

    @Value("${app.producer.partition-key}")
    private String partitionKeyField;

    /**
     * Produce a single order event with reliable delivery
     */
    public void sendOrderEvent(OrderEvent event) {
        try {
            // Use order_id as partition key to maintain ordering per order
            String partitionKey = event.getOrderId();
            
            Message<Object> message = MessageBuilder
                .withPayload((Object) event)
                .setHeader(KafkaHeaders.TOPIC, orderEventsTopic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, partitionKey)
                .build();

            kafkaTemplate.send(message).whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("✓ Message sent: topic={}, partition={}, offset={}, key={}", 
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        partitionKey);
                } else {
                    log.error("✗ Failed to send message for order: {}", event.getOrderId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error producing order event: {}", event.getOrderId(), e);
        }
    }

    /**
     * Produce multiple order events in batch
     */
    public void sendOrderEventsBatch(List<OrderEvent> events) {
        log.info("Sending batch of {} order events", events.size());
        events.forEach(this::sendOrderEvent);
        log.info("Batch sent successfully");
    }
}
