package com.ecommerceflow.producer.config;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.producer.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Bean
    public ProducerFactory<String, SpecificRecord> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Broker connection
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Serialization
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        configProps.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        configProps.put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, true);

        // Reliability settings
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // Performance tuning
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        // Timeout settings
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, SpecificRecord> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
