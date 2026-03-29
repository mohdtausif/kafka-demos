package org.example.kafkajavademo1;
import org.apache.kafka.clients.producer.*;
import java.util.Properties;

public class BasicProducer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        ProducerRecord<String, String> record = new ProducerRecord<>("demo-topic", "key1", "Hello Kafka this is Producer demo!_ Mohd Tausif");
        producer.send(record);
        producer.close();
    }
}
