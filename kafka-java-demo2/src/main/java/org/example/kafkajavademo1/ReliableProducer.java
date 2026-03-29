package org.example.kafkajavademo1;

import org.apache.kafka.clients.producer.*;
import java.util.Properties;

public class ReliableProducer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        props.put("acks", "all");
        props.put("retries", Integer.MAX_VALUE);
        props.put("enable.idempotence", "true");
        props.put("max.in.flight.requests.per.connection", 1);

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        for (int i = 0; i < 10; i++) {
            producer.send(new ProducerRecord<>("demo-topic", "safeKey", "Reliable-" + i));
        }
        producer.close();
    }
}
