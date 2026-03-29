package org.example.kafkajavademo1;

import org.apache.kafka.clients.producer.*;
import java.util.Properties;

public class ThroughputProducer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        props.put("acks", "0");
        props.put("compression.type", "snappy");
        props.put("batch.size", 32768);
        props.put("linger.ms", 20);

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        for (int i = 0; i < 100; i++) {
            producer.send(new ProducerRecord<>("demo-topic", "fastKey", "Fast-" + i));
        }
        producer.close();
    }
}
