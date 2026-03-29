# Kafka Capstone Project - Java Spring Boot Implementation

Complete Java/Spring Boot implementation of the Kafka Capstone Project with event-driven order processing pipeline.

## 📋 Project Structure

```
kafka-java/
├── pom.xml                          # Maven configuration with Kafka dependencies
├── src/main/
│   ├── java/com/ecommerceflow/
│   │   ├── KafkaCapstoneApplication.java   # Spring Boot main class
│   │   ├── config/
│   │   │   └── KafkaConfig.java    # Kafka producer/consumer factory configs
│   │   ├── controller/
│   │   │   └── OrderEventController.java   # REST endpoints for producer & metrics
│   │   ├── model/
│   │   │   ├── OrderEvent.java     # Order event domain model
│   │   │   └── OrderMetrics.java   # Analytics metrics model
│   │   ├── producer/
│   │   │   ├── OrderProducerService.java   # Kafka producer service
│   │   │   └── OrderEventGenerator.java    # Sample data generator
│   │   └── consumer/
│   │       ├── AnalyticsConsumerService.java    # Analytics consumer
│   │       ├── NotificationConsumerService.java # Notification consumer with DLQ
│   │       └── InventoryConsumerService.java    # Inventory consumer
│   ├── avro/
│   │   └── OrderEventAvro.avsc     # Avro schema for order events
│   └── resources/
│       └── application.yml         # Spring Boot configuration
```

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Kafka cluster running (from docker-compose.yml in parent directory)

### 1. Build the Project
```bash
cd kafka-java
mvn clean package
```

### 2. Run the Application
```bash
mvn spring-boot:run
```

The application will start on port 8080 with:
- REST endpoints for producer and metrics
- 3 consumer services listening to order events
- Spring Kafka auto-configuration

### 3. Use REST API to Send Events

**Send Single Event:**
```bash
curl -X POST "http://localhost:8080/api/producer/send?eventType=ORDER_CREATED"
```

**Send Batch of 100 Events:**
```bash
curl -X POST "http://localhost:8080/api/producer/send-batch?count=100"
```

**Send Complete Order Lifecycle:**
```bash
curl -X POST "http://localhost:8080/api/producer/send-lifecycle"
```

### 4. View Metrics

**Analytics Metrics:**
```bash
curl http://localhost:8080/api/metrics/analytics
```

**Inventory Status:**
```bash
curl http://localhost:8080/api/metrics/inventory
```

**Notification Statistics:**
```bash
curl http://localhost:8080/api/metrics/notifications
```

**Health Check:**
```bash
curl http://localhost:8080/api/health
```

## 🏗️ Architecture

### Producer (`OrderProducerService`)
- Uses Spring `KafkaTemplate` for reliable message delivery
- Configuration:
  - `acks=all` - Wait for all in-sync replicas
  - `idempotence=true` - Exactly-once semantics
  - `compression=snappy` - Message compression
- Partitions by `order_id` to maintain ordering per customer
- Async acknowledgment with callbacks

### Consumers (3 Services)

**AnalyticsConsumerService**
- Consumer group: `analytics-consumer-group`
- Aggregates order metrics (count, revenue, by-type)
- Manual offset commit for exactly-once semantics
- Real-time metrics updates

**NotificationConsumerService**
- Consumer group: `notification-consumer-group`
- Sends notifications for each order event
- Dead Letter Queue support for failed notifications
- Maintains statistics on processed/failed messages

**InventoryConsumerService**
- Consumer group: `inventory-consumer-group`
- Manages inventory reservations (ORDER_CREATED)
- Deducts from stock (ORDER_SHIPPED)
- Tracks stock levels per product

### Configuration (`KafkaConfig`)
- Producer factory with Avro serializer
- Consumer factory with Avro deserializer
- Schema Registry integration
- Manual commit mode for exactly-once semantics
- Concurrent consumer processing (3 threads)

## 📊 REST API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/producer/send` | Send single order event |
| POST | `/api/producer/send-batch` | Send batch of events |
| POST | `/api/producer/send-lifecycle` | Send complete order lifecycle |
| GET | `/api/metrics/analytics` | Get analytics metrics |
| GET | `/api/metrics/inventory` | Get inventory status |
| GET | `/api/metrics/inventory/{productId}` | Get product inventory |
| GET | `/api/metrics/notifications` | Get notification stats |
| GET | `/api/health` | Health check |

## 🔧 Configuration

### application.yml Details

**Kafka Producer Settings:**
- `bootstrap-servers`: List of broker addresses
- `acks=all`: Wait for all in-sync replicas
- `retries=3`: Automatic retry on failure
- `compression.type=snappy`: Compress messages
- `linger.ms=10`: Wait 10ms for batching
- `batch.size=32768`: Batch 32KB messages

**Kafka Consumer Settings:**
- `auto.offset.reset=earliest`: Start from beginning
- `enable.auto.commit=false`: Manual offset management
- `max.poll.records=100`: Fetch 100 records per poll
- `session.timeout.ms=60000`: Session heartbeat timeout

**Schema Registry:**
- URL: `http://localhost:8081`
- Used for Avro serialization/deserialization
- Automatic schema discovery and validation

## 🧪 Testing

### Run Unit Tests
```bash
mvn test
```

### Manual Integration Test
```bash
# Terminal 1: Start application
mvn spring-boot:run

# Terminal 2: Send some events
for i in {1..10}; do
  curl -X POST "http://localhost:8080/api/producer/send?eventType=ORDER_CREATED"
  sleep 1
done

# Terminal 2: Check metrics
curl http://localhost:8080/api/metrics/analytics | jq '.'
```

## 📈 Performance Tuning

### Producer Tuning
```yaml
# Increase throughput
batch.size: 65536        # 64KB batches
linger.ms: 50            # Wait 50ms for batching
compression.type: lz4    # Faster compression
```

### Consumer Tuning
```yaml
# Increase parallelism
concurrency: 10          # 10 consumer threads
max.poll.records: 500    # Fetch 500 records
fetch.min.bytes: 10240   # Wait for 10KB
```

## 🔒 Security Configuration (Optional)

For production, add SASL/SSL configuration:

```yaml
spring:
  kafka:
    security:
      protocol: SASL_SSL
    properties:
      sasl.mechanism: SCRAM-SHA-256
      sasl.jaas.config: |
        org.apache.kafka.common.security.scram.ScramLoginModule required
        username="producer" password="secret";
```

## 📝 Logging

Application uses SLF4J with Logback. Configure in `application.yml`:

```yaml
logging:
  level:
    com.ecommerceflow: DEBUG
    org.apache.kafka: WARN
```

## 🐛 Troubleshooting

### Connection Error: "Failed to construct kafka client"
- Verify Kafka brokers are running
- Check `bootstrap-servers` configuration
- Verify network connectivity

### Serialization Error: "Schema not found"
- Ensure Schema Registry is running on port 8081
- Register schemas before sending events
- Check `schema.registry.url` configuration

### Consumer Not Receiving Messages
- Check consumer group status: `kafka-consumer-groups`
- Verify topic exists: `kafka-topics --list`
- Check consumer logs for errors

### High Memory Usage
- Reduce `max.poll.records` in consumer config
- Reduce `batch.size` in producer config
- Lower consumer `concurrency` setting

## 📚 Dependencies

Key Maven dependencies:
- `spring-boot-starter-web` - Web MVC framework
- `spring-kafka` - Kafka integration
- `kafka-avro-serializer` - Confluent Avro support
- `lombok` - Boilerplate reduction
- `jackson` - JSON processing

## 🚀 Deployment

### JAR Packaging
```bash
mvn clean package
java -jar target/kafka-capstone-1.0.0.jar
```

### Docker Deployment
```dockerfile
FROM openjdk:17-slim
COPY target/kafka-capstone-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 📖 References

- [Spring Kafka](https://spring.io/projects/spring-kafka)
- [Confluent Java Clients](https://docs.confluent.io/kafka-clients/java/current/overview.html)
- [Avro Documentation](https://avro.apache.org/docs/current/)
- [Kafka Documentation](https://kafka.apache.org/documentation/)

---

**Status**: Production-ready | **Version**: 1.0 | **Last Updated**: 2024
