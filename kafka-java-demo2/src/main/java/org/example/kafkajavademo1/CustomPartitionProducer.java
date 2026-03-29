package org.example.kafkajavademo1;

import org.apache.kafka.clients.producer.*;
import java.util.Properties;

public class CustomPartitionProducer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        // add the below line to tekll Kafka to use your custom partioner
        props.put("partitioner.class", "com.example.kafka.CustomPartitioner");

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        producer.send(new ProducerRecord<>("demo-topic", "VIP123", "VIP message"));
        producer.send(new ProducerRecord<>("demo-topic", "User456", "Regular message"));
        producer.close();
    }
}
