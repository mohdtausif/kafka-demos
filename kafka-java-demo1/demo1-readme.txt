c:\Demo 01 Hello World.. Step by step
Plz find the step‑by‑step “Hello World” demo in Java using Apache Kafka: you’ll set up Kafka locally, create a producer to send messages, and a consumer to read them back. This is the simplst way to understand Kafka’s publish/subscribe model.
 Prerequisites
	•	Java 8+ installed (JDK 8 or higher).
	•	Apache Kafka running locally (either via direct install or Docker).
	•	Apache Maven for dependency management.
	•	Zookeeper (if not using Kafka’s newer KRaft mode).
 Project Setup
	•	Create a Maven project Add Kafka client dependency in pom.xml:
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.7.0</version>
</dependency>
2.  Start Kafka locally
	•	Start Zookeeper:
bin/zookeeper-server-start.sh config/zookeeper.properties
3. Start Kafka broker:
bin/kafka-server-start.sh config/server.properties
4. Create a Kafka topic
bin/kafka-topics.sh --create --topic hello-world --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
docker exec -it kafka-demos-kafka-1 kafka-topics --create --topic hello-world --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1


 Step 1: Kafka Producer (Send Data)
Create HelloProducer.java file
import org.apache.kafka.clients.producer.*;

import java.util.Properties;

public class HelloProducer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        Producer<String, String> producer = new KafkaProducer<>(props);

        ProducerRecord<String, String> record = new ProducerRecord<>("hello-world", "key1", "Hello Kafka!");
        producer.send(record);

        producer.close();
        System.out.println("Message sent successfully!");
    }
}

 Step 2: Kafka Consumer (Receive Data)
Create HelloConsumer.java file 

import org.apache.kafka.clients.consumer.*;
import java.util.Collections;
import java.util.Properties;

public class HelloConsumer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "test-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("hello-world"));

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(java.time.Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records) {
                System.out.printf("Received message: %s%n", record.value());
            }
        }
    }
}
 Running the Demo
	•	Run HelloProducer 
	•	Run HelloConsumer.

—-------------------------------------   __________________________________   —-----------------

Or you can follow this … 

Kafka Demo
1. 
open cmd in admin mode

inside kafka folder run zookeeper

c:\kafka>.\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties

2.

open cmd in admin mode again 

.\bin\windows\kafka-server-start.bat .\config\server.properties

3. create topic

C:\kafka>.\bin\windows\kafka-topics.bat --create --topic hello-topic1 --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
Created topic hello-topic1.


4.
- Run producer (your Java code).

package org.example;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class HelloKafkaProducer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        Producer<String, String> producer = new KafkaProducer<>(props);
        producer.send(new ProducerRecord<>("hello-topic2", "key1", "Hi Kaushal!"));
        producer.close();
    }
}

5. - Run consumer (command above).

package org.example;
import org.apache.kafka.clients.consumer.*;
import java.util.Collections;
import java.util.Properties;
public class HelloKafkaConsumer {
    public static void main(String[] args) {
        // TODO: code here
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "hello-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("hello-topic1"));

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(java.time.Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records) {
                System.out.printf("Received message: %s%n", record.value());
            }
        }
    }
}


open cmd in admin mode again 
in kafka folder 
c:\kafka> 

.\bin\windows\kafka-console-consumer.bat --topic hello-topic --bootstrap-server localhost:9092 --from-beginning

ctrl+c for exit 

pom.xml for your reference:

<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.example</groupId>
    <artifactId>hello-kafka</artifactId>
    <version>1.0-SNAPSHOT</version>

    <dependencies>
        <dependency>
            <groupId>org.apache.kafka</groupId>
            <artifactId>kafka-clients</artifactId>
            <version>3.4.0</version>
        </dependency>
    </dependencies>
</project>


note:
When you run the Kafka console consumer in CMD, it will keep listening for new messages until you stop it manually. To exit cleanly:
- Press Ctrl + C in the Command Prompt window.
This sends an interrupt signal and stops the consumer process.

note : ensure topic names match.. 





