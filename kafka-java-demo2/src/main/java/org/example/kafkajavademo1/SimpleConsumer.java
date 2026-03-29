package org.example.kafkajavademo1;

import org.apache.kafka.clients.consumer.*;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class SimpleConsumer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "demo-consumer-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("demo-topic"));

        while (true) {
            for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(100))) {
                System.out.printf("Partition=%d Key=%s Value=%s%n",
                        record.partition(), record.key(), record.value());
            }
        }
    }
}
