package org.example.kafkajavademo1;

import org.apache.kafka.clients.producer.*;
import java.util.Properties;
public class AcksProducer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        // Change acks to "0", "1", or "all" for demo
        props.put("acks", "all");

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        for (int i = 0; i < 5; i++) {
            producer.send(new ProducerRecord<>("demo-topic", "key" + i, "Message-" + i));
        }
        producer.close();
    }
}
