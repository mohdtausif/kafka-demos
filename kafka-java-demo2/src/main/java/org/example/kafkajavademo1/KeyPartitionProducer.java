package org.example.kafkajavademo1;

import org.apache.kafka.clients.producer.*;
import java.util.Properties;

public class KeyPartitionProducer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        for (int i = 0; i < 5; i++) {
            producer.send(new ProducerRecord<>("demo-topic", "orderKey", "Ordered-" + i));
        }
        producer.close();
    }
}
