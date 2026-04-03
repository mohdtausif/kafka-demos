package com.ecommerceflow.streams.topology;

import com.ecommerceflow.avro.OrderCreated;
import com.ecommerceflow.avro.OrderPaid;
import com.ecommerceflow.common.KafkaTopics;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

/**
 * Kafka Streams topology for real-time order analytics.
 *
 * Implements:
 * 1. Stateless transformations: filter orders by status
 * 2. Tumbling window: order count per hour
 * 3. Tumbling window: revenue aggregation per hour
 * 4. KStream-KTable join: enrich orders with customer data
 */
@Slf4j
@Configuration
public class OrderAnalyticsTopology {

    @Value("${spring.kafka.streams.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Bean
    public KStream<String, OrderCreated> orderAnalyticsStream(StreamsBuilder builder) {
        // Configure Avro Serdes
        Map<String, String> serdeConfig = Collections.singletonMap("schema.registry.url", schemaRegistryUrl);

        SpecificAvroSerde<OrderCreated> orderCreatedSerde = new SpecificAvroSerde<>();
        orderCreatedSerde.configure(serdeConfig, false);

        SpecificAvroSerde<OrderPaid> orderPaidSerde = new SpecificAvroSerde<>();
        orderPaidSerde.configure(serdeConfig, false);

        // ========================================
        // 1. Read order.created stream
        // ========================================
        KStream<String, OrderCreated> orderCreatedStream = builder.stream(
                KafkaTopics.ORDER_CREATED,
                Consumed.with(Serdes.String(), orderCreatedSerde)
        );

        // Log all incoming orders
        orderCreatedStream.peek((key, value) ->
                log.info("[STREAMS] Processing order: orderId={}, customerId={}, productId={}",
                        value.getOrderId(), value.getCustomerId(), value.getProductId())
        );

        // ========================================
        // 2. Tumbling Window: Order Count per Hour
        // ========================================
        KStream<String, String> orderCountStream = orderCreatedStream
                .groupBy((key, value) -> "all-orders", Grouped.with(Serdes.String(), orderCreatedSerde))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofHours(1)))
                .count(Materialized.<String, Long, WindowStore<Bytes, byte[]>>as("order-count-per-hour-store")
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long()))
                .toStream()
                .<String, String>map((windowedKey, count) -> {
                    String windowKey = String.format("window_%d_%d",
                            windowedKey.window().start(), windowedKey.window().end());
                    log.info("[STREAMS] Order count window [{} - {}]: count={}",
                            windowedKey.window().startTime(), windowedKey.window().endTime(), count);
                    return KeyValue.pair(windowKey, count != null ? count.toString() : "0");
                });

        orderCountStream.to(KafkaTopics.ANALYTICS_ORDER_COUNTS, Produced.with(Serdes.String(), Serdes.String()));

        // ========================================
        // 3. Order Count by Product (Tumbling Window)
        // ========================================
        orderCreatedStream
                .groupBy((key, value) -> value.getProductId().toString(),
                        Grouped.with(Serdes.String(), orderCreatedSerde))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofHours(1)))
                .count(Materialized.<String, Long, WindowStore<Bytes, byte[]>>as("product-order-count-store")
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long()))
                .toStream()
                .peek((windowedKey, count) ->
                        log.info("[STREAMS] Product order count: product={}, window=[{} - {}], count={}",
                                windowedKey.key(), windowedKey.window().startTime(),
                                windowedKey.window().endTime(), count));

        // ========================================
        // 4. Revenue Aggregation per Hour (from order.paid)
        // ========================================
        KStream<String, OrderPaid> orderPaidStream = builder.stream(
                KafkaTopics.ORDER_PAID,
                Consumed.with(Serdes.String(), orderPaidSerde)
        );

        KStream<String, String> revenueStream = orderPaidStream
                .groupBy((key, value) -> "all-revenue", Grouped.with(Serdes.String(), orderPaidSerde))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofHours(1)))
                .aggregate(
                        () -> "0.00",
                        (key, orderPaid, currentRevenue) -> {
                            BigDecimal current = new BigDecimal(currentRevenue);
                            BigDecimal amount = bytesToDecimal(orderPaid.getTotalAmount());
                            BigDecimal newRevenue = current.add(amount);
                            log.info("[STREAMS] Revenue aggregation: orderId={}, amount={}, totalInWindow={}",
                                    orderPaid.getOrderId(), amount, newRevenue);
                            return newRevenue.toString();
                        },
                        Materialized.<String, String, WindowStore<Bytes, byte[]>>as("revenue-per-hour-store")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(Serdes.String())
                )
                .toStream()
                .<String, String>map((windowedKey, revenue) -> {
                    String windowKey = String.format("revenue_%d_%d",
                            windowedKey.window().start(), windowedKey.window().end());
                    return KeyValue.pair(windowKey, revenue);
                });

        revenueStream.to(KafkaTopics.ANALYTICS_REVENUE, Produced.with(Serdes.String(), Serdes.String()));

        // ========================================
        // 5. KStream-KTable Join: Enrich orders with customer data
        // ========================================
        KTable<String, String> customerTable = builder.table(
                KafkaTopics.CUSTOMERS,
                Consumed.with(Serdes.String(), Serdes.String()),
                Materialized.as("customer-store")
        );

        orderCreatedStream
                .selectKey((key, value) -> value.getCustomerId().toString())
                .leftJoin(
                        customerTable,
                        (order, customerData) -> {
                            String enriched = String.format(
                                    "{\"orderId\":\"%s\",\"customerId\":\"%s\",\"productId\":\"%s\",\"quantity\":%d,\"customerData\":%s}",
                                    order.getOrderId(), order.getCustomerId(), order.getProductId(),
                                    order.getQuantity(), customerData != null ? customerData : "null");
                            log.info("[STREAMS] Enriched order with customer data: orderId={}, hasCustomerData={}",
                                    order.getOrderId(), customerData != null);
                            return enriched;
                        },
                        Joined.with(Serdes.String(), orderCreatedSerde, Serdes.String())
                )
                .peek((key, value) ->
                        log.debug("[STREAMS] Enriched order output: key={}", key));

        return orderCreatedStream;
    }

    private BigDecimal bytesToDecimal(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new BigDecimal(new BigInteger(bytes), 2);
    }
}
