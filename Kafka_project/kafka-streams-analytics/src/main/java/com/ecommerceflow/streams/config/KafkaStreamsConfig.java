package com.ecommerceflow.streams.config;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaStreamsConfig {

    @Value("${spring.kafka.streams.application-id}")
    private String applicationId;

    @Value("${spring.kafka.streams.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.streams.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Bean(name = "defaultKafkaStreamsConfig")
    public KafkaStreamsConfiguration kStreamsConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10485760);
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 2);
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, 3);
        props.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streams");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put("schema.registry.url", schemaRegistryUrl);
        return new KafkaStreamsConfiguration(props);
    }
}
