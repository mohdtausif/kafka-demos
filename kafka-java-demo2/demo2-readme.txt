Demo 02 - Module 2 – Kafka Producers & Message Publishing
Project Layout


 pom.xml
<dependency>
   <groupId>org.apache.kafka</groupId>
   <artifactId>kafka-clients</artifactId>
   <version>3.7.0</version>
</dependency>

 BasicProducer.java
package com.example.kafka;
import org.apache.kafka.clients.producer.*;
import java.util.Properties;

public class BasicProducer {
   public static void main(String[] args) {
       Properties props = new Properties();
       props.put("bootstrap.servers", "localhost:9092");
       props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
       props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

       KafkaProducer<String, String> producer = new KafkaProducer<>(props);
       ProducerRecord<String, String> record = new ProducerRecord<>("demo-topic", "key1", "Hello Kafka this is Producer demo!");
       producer.send(record);
       producer.close();
   }
}

 AcksProducer.java
package com.example.kafka;
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

KeyPartitionProducer.java
package com.example.kafka;
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

 CustomPartitioner.java
package com.example.kafka;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import java.util.Map;

public class CustomPartitioner implements Partitioner {
   @Override
   public int partition(String topic, Object keyObj, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
       String key = (String) keyObj;
       int numPartitions = cluster.partitionCountForTopic(topic);

       if (key.startsWith("VIP")) {
           return 0; // VIP messages always go to partition 0
       } else {
           return Math.abs(key.hashCode()) % numPartitions;
       }
   }
   @Override public void close() {}
   @Override public void configure(Map<String, ?> configs) {}
}

CustomPartitionProducer.java
package com.example.kafka;
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


ThroughputProducer.java
package com.example.kafka;
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
 ReliableProducer.java
package com.example.kafka;
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


SimpleConsumer.java
package com.example.kafka;
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

To Run
	•	Start ZooKeeper and Kafka broker.
open cmd in admin mode
inside kafka folder run zookeeper
c:\kafka>.\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties

Kafka broker also
open cmd in admin mode again 
.\bin\windows\kafka-server-start.bat .\config\server.properties
	•	Create topic:
open cmd in admin mode again 
C:\kafka>.\bin\windows\kafka-topics.bat --create --topic demo-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
docker exec -it kafka-demos-kafka-1 kafka-topics  --create --topic demo-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

3.  Run consumer: in the same window 

C:\kafka>.\bin\windows\kafka-console-consumer.bat --topic demo-topic --bootstrap-server localhost:9092 --from-beginning --property print.key=true --property print.partition=true
docker exec -it kafka-demos-kafka-1 kafka-console-consumer  --topic demo-topic --bootstrap-server localhost:9092 --from-beginning --property print.key=true --property print.partition=true


To run the above demo on your machine 
Running Each Module
1. Basic Producer
	•	Compile and run BasicProducer.java 
	•	Verify with a console consumer:
C:\kafka>.\bin\windows\kafka-topics.bat --topic demo-topic --bootstrap-server localhost:9092 --from-beginning
2. Acks Demo
	•	Change props.put(“acks”, “0|1|all”) in code. customPartitionProduer java file
	•	Run producer three times.
	•	Observe consumer output and latency differences.
3. Key-Based Partitioning
	•	Send multiple messages with the same key.
	•	Run consumer with the below command 
C:\kafka>.\bin\windows\kafka-topics.bat --topic demo-topic --bootstrap-server localhost:9092 --from-beginning --property print.key=true --property print.partition=true
	•	 You wil see all messages with same key routed to the same partition.
4. Custom Partitioner
	•	Implement Cutompartitioner.java
	•	Add to producer config:
	•	props.put("partitioner.class", "com.example.CustomPartitioner"); Run producer with keys like “VIP123” and “user456”.
	•	Consumer will show VIP messages always in partition 0.
5. Observe Throughput vs Reliability
	•	Run producer twice with
	•	High throughput config (acks=0, compression, batching).
	•	High reliability config (acks=all, idempotence, retries).
	•	Also we can compare speed vs guaranteed delivery.



