# Kafka Capstone Project - Complete Event-Driven Pipeline

A production-ready Kafka implementation demonstrating an e-commerce order processing pipeline with **Java Spring Boot**, multiple consumer services, Kafka Connect integrations, real-time analytics, and comprehensive monitoring.

## 📋 Project Contents

### Core Components
- **Kafka Cluster** (`docker-compose.yml`)
  - 3-broker Kafka cluster with Zookeeper for HA
  - Confluent Schema Registry v7.3.4
  - Kafka Connect in distributed mode
  - Prometheus + Grafana monitoring stack

- **Java Spring Boot Application** (`kafka-java/`)
  - Spring Boot 3.1.5 with Spring Kafka
  - Order event producer service with reliable configuration
  - 3 consumer services: Analytics, Notifications (with DLQ), Inventory
  - REST API for event production and metrics retrieval
  - Avro serialization with Schema Registry integration
  - Manual offset management for exactly-once semantics

- **Avro Schemas** (`schemas/` & `kafka-java/src/main/avro/`)
  - `order-event.avsc` - Order lifecycle events with full order details
  - `customer.avsc` - Customer reference data for KTable joins
  - `OrderEventAvro.avsc` - Java-compiled schema

- **Configuration & Scripts** (`config/`)
  - `create-topics.sh` - Automated topic creation with proper replication

- **Kafka Connect** (`connect/`)
  - File source connector configuration
  - File sink connector configuration for output topics

- **Monitoring** (`monitoring/`)
  - `prometheus.yml` - JMX metrics collection from brokers
  - `alerts.yml` - Alert rules for operational issues

- **Documentation** (`docs/`)
  - `ARCHITECTURE_AND_DEPLOYMENT.md` - Complete architecture & deployment guide
  - `OPERATIONAL_RUNBOOK.md` - Production operational procedures
  - `SECURITY_GUIDE.md` - SSL/TLS, SASL, ACL configurations

## 🚀 Quick Start

### Prerequisites
```bash
# Required:
- Docker & Docker Compose
- Java 17+
- Maven 3.8+
- ~2GB available disk space
```

### 1. Start Kafka Cluster
```bash
cd Kafka-Capstone-Project
docker-compose up -d

# Wait for services to start (30-60 seconds)
sleep 30

# Verify all containers running
docker-compose ps
```

### 2. Create Topics
```bash
chmod +x config/create-topics.sh
./config/create-topics.sh
```

### 3. Build Java Spring Boot Application
```bash
cd kafka-java
mvn clean package -DskipTests
```

### 4. Run Spring Boot Application (Terminal 1)
```bash
cd kafka-java
mvn spring-boot:run
# Application listens on http://localhost:8080
```

### 5. Send Order Events (Terminal 2)
```bash
# Send single ORDER_CREATED event
curl -X POST "http://localhost:8080/api/producer/send?eventType=ORDER_CREATED"

# Send batch of 100 events with various types
curl -X POST "http://localhost:8080/api/producer/send-batch?count=100"

# Send complete order lifecycle (4 events)
curl -X POST "http://localhost:8080/api/producer/send-lifecycle"
```

### 6. Monitor Metrics (Terminal 2)
```bash
# View analytics metrics
curl http://localhost:8080/api/metrics/analytics | jq '.'

# View inventory status
curl http://localhost:8080/api/metrics/inventory | jq '.'

# View notification statistics
curl http://localhost:8080/api/metrics/notifications | jq '.'
```

## 📊 Access Dashboards & Services

| Component | URL | Credentials |
|-----------|-----|-------------|
| Spring Boot App | http://localhost:8080 | - |
| Grafana | http://localhost:3000 | admin/admin |
| Prometheus | http://localhost:9090 | - |
| Schema Registry | http://localhost:8081 | - |
| Kafka Connect | http://localhost:8083 | - |

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────┐
│ Spring Boot Application (Java)      │
│ - OrderProducerService              │
│ - AnalyticsConsumerService          │
│ - NotificationConsumerService       │
│ - InventoryConsumerService          │
└──────────────────┬──────────────────┘
                   │
                   ▼
        ┌──────────────────────┐
        │  Order Events Topic  │
        │ (3 parts, RF=2)      │
        └─────────┬────────────┘
                  │
    ┌─────────────┼──────────────┐
    │             │              │
    ▼             ▼              ▼
 Analytics  Notifications  Inventory
 Consumer   Consumer(DLQ)  Consumer
    │             │              │
    ▼             ▼              ▼
 Metrics    Notifications   Stock Updates
  
┌─────────────────────────────────────┐
│ Monitoring Layer                    │
│ JMX → Prometheus → Grafana          │
└─────────────────────────────────────┘
```

## 🔧 Configuration Highlights

### Producer (Spring Kafka)
- Serializer: `KafkaAvroSerializer` via Schema Registry
- `acks=all` - Wait for all in-sync replicas
- `enable.idempotence=true` - Exactly-once semantics
- `compression.type=snappy` - Reduce network overhead
- Partitioned by `order_id` - Maintain message order per customer
- Async callbacks for delivery confirmation

### Consumers (3 Spring Kafka Services)
- Deserializer: `KafkaAvroDeserializer` with Schema Registry
- Manual offset commit (`MANUAL_IMMEDIATE`) for exactly-once
- Concurrent processing: 3 consumer threads
- Dead Letter Queue support in notification consumer
- Consumer groups: `analytics-consumer-group`, `notification-consumer-group`, `inventory-consumer-group`

### Topics
- `order-events`: 3 partitions, RF=2, 7-day retention
- `order-analytics`: 3 partitions, RF=2, 30-day retention  
- `order-events-dlq`: 1 partition, RF=2, DLQ handling

## 📊 Key Features Demonstrated

✅ **Event-Driven Architecture** - Complete order lifecycle event stream  
✅ **Reliable Producer** - Idempotent with acks=all configuration  
✅ **Multiple Consumers** - 3 services with different business logic  
✅ **Schema Management** - Avro schemas with Schema Registry  
✅ **Error Handling** - Dead Letter Queue with retry mechanics  
✅ **Monitoring** - Prometheus metrics + Grafana dashboards  
✅ **Fault Tolerance** - Multi-broker replication (RF≥2)  
✅ **Production Ready** - Spring Boot best practices, comprehensive logging  

## 📚 Documentation

- **Java Implementation**: See [kafka-java/README.md](kafka-java/README.md) for detailed setup
- **Architecture**: See [docs/ARCHITECTURE_AND_DEPLOYMENT.md](docs/ARCHITECTURE_AND_DEPLOYMENT.md)
- **Operations**: See [docs/OPERATIONAL_RUNBOOK.md](docs/OPERATIONAL_RUNBOOK.md)
- **Security**: See [docs/SECURITY_GUIDE.md](docs/SECURITY_GUIDE.md)

### Key Sections
- Multi-broker cluster setup with high availability
- Producer/consumer implementations with Avro serialization
- Topic partitioning strategy for ordering guarantees
- Monitoring and alerting configuration
- Operational procedures and troubleshooting
- Security configurations (SSL/TLS, SASL, ACLs)

## 🧪 Testing & Validation

### Build & Run Tests
```bash
cd kafka-java
mvn test
```

### Integration Testing
```bash
# Terminal 1: Start application
mvn spring-boot:run

# Terminal 2: Send test events
curl -X POST "http://localhost:8080/api/producer/send-batch?count=50"

# Terminal 2: Check consumer group status
docker exec kafka-demos-kafka1-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group analytics-consumer-group \
  --describe
```

## 🚨 Common Commands

### Kafka Topic Management
```bash
# List all topics
docker exec kafka-demos-kafka1-1 kafka-topics --list --bootstrap-server localhost:9092

# Describe specific topic with replication info
docker exec kafka-demos-kafka1-1 kafka-topics --describe --topic order-events --bootstrap-server localhost:9092

# Check consumer group lag
docker exec kafka-demos-kafka1-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group analytics-consumer-group \
  --describe

# Reset consumer group offset
docker exec kafka-demos-kafka1-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group analytics-consumer-group \
  --reset-offsets --to-latest --execute
```

### Spring Boot Application Management
```bash
# Build with Maven
mvn clean package

# Run with Spring Boot Maven plugin
mvn spring-boot:run

# Run compiled JAR
java -jar target/kafka-capstone-1.0.0.jar

# Build Docker image
docker build -t kafka-capstone:latest kafka-java/
```

### Schema Registry Management
```bash
# List registered schemas
curl http://localhost:8081/subjects

# Get schema versions
curl http://localhost:8081/subjects/order-event-value/versions

# Register order event schema
curl -X POST http://localhost:8081/subjects/order-event-value/versions \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d @schemas/order-event.avsc
```

### Monitoring & Metrics
```bash
# Check Kafka broker JMX metrics
curl http://localhost:9101/metrics | grep kafka_server

# View consumer lag via API
curl http://localhost:8080/api/metrics/analytics | jq '.totalOrders'

# Check Prometheus scrape targets
curl http://localhost:9090/api/v1/targets

# Prometheus query: broker leader count
curl 'http://localhost:9090/api/v1/query?query=kafka_controller_kafkacontroller_globalpartitioncount'
```

## 🛑 Shutdown & Cleanup

```bash
# Graceful shutdown (preserves Kafka data volumes)
docker-compose down

# Full cleanup with data removal
docker-compose down -v

# Stop Spring Boot application
# Press Ctrl+C in the terminal where mvn spring-boot:run is running
```

## 🔒 Security Considerations

For production deployment, implement:
1. **SSL/TLS Encryption** - Broker-to-broker and client-to-broker communication
2. **SASL Authentication** - User authentication (PLAIN or SCRAM-SHA-256)
3. **ACL Policies** - Topic and consumer group level access control
4. **Client Quotas** - Rate limiting to prevent resource exhaustion
5. **Monitoring Alerts** - Automated detection of security anomalies

See [docs/SECURITY_GUIDE.md](docs/SECURITY_GUIDE.md) for detailed SSL/TLS and SASL configurations.

## 📈 Performance Tuning

### Producer Optimization
```yaml
batch.size: 65536           # 64KB batches for throughput
linger.ms: 50               # Wait 50ms for batching
compression.type: lz4       # Faster compression
buffer.memory: 67108864     # 64MB buffer
```

### Consumer Optimization
```yaml
concurrency: 10             # 10 parallel consumer threads
max.poll.records: 500       # Fetch 500 records per poll
fetch.min.bytes: 10240      # Wait for 10KB minimum
fetch.max.wait.ms: 500      # Max 500ms wait
```

See [docs/ARCHITECTURE_AND_DEPLOYMENT.md](#performance-tuning) for detailed tuning guide.

## 🐛 Troubleshooting Guide

### Spring Boot App Won't Connect to Kafka
**Error**: "Failed to resolve broker address"

**Solution**:
```bash
# Verify Kafka containers are running
docker-compose ps

# Check broker connectivity
docker exec kafka-demos-kafka1-1 kafka-broker-api-versions --bootstrap-server localhost:9091

# Review Spring Boot logs for connection errors
```

### Consumer Groups Not Consuming Messages
**Error**: "No messages processed by consumer"

**Solution**:
```bash
# Verify topics exist
docker exec kafka-demos-kafka1-1 kafka-topics --list --bootstrap-server localhost:9092

# Check consumer group status
docker exec kafka-demos-kafka1-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group analytics-consumer-group \
  --describe

# Verify Schema Registry is accessible
curl http://localhost:8081/subjects
```

### High Consumer Lag
**Error**: "Consumer lag continuously increasing"

**Solution**:
```bash
# Check consumer processing rate from API
curl http://localhost:8080/api/metrics/analytics | jq '.'

# Scale up consumers by increasing concurrency in application.yml
# Restart application for changes to take effect

# Reset offsets if needed
docker exec kafka-demos-kafka1-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group analytics-consumer-group \
  --reset-offsets --to-latest --execute
```

See [docs/OPERATIONAL_RUNBOOK.md#troubleshooting](docs/OPERATIONAL_RUNBOOK.md#troubleshooting) for more detailed troubleshooting procedures.

## 📚 Complete Documentation

| Document | Purpose |
|----------|---------|
| [kafka-java/README.md](kafka-java/README.md) | Java implementation details & API reference |
| [docs/ARCHITECTURE_AND_DEPLOYMENT.md](docs/ARCHITECTURE_AND_DEPLOYMENT.md) | Complete architecture, design decisions, deployment guide |
| [docs/OPERATIONAL_RUNBOOK.md](docs/OPERATIONAL_RUNBOOK.md) | Production operational procedures, recovery steps |
| [docs/SECURITY_GUIDE.md](docs/SECURITY_GUIDE.md) | SSL/TLS, SASL/SCRAM, ACL configurations |

## 📖 External References

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Confluent Cloud Documentation](https://docs.confluent.io/)
- [Spring Kafka Project](https://spring.io/projects/spring-kafka)
- [Kafka Best Practices](https://kafka.apache.org/documentation/#bestpractices)
- [Schema Registry Guide](https://docs.confluent.io/platform/current/schema-registry/)

## ✅ Capstone Requirements Coverage

This project fully implements the **Kafka Capstone Project** requirements:

| Requirement | Implementation | Status |
|-------------|-----------------|--------|
| **Architecture Design** | 3-broker Kafka cluster with Zookeeper | ✅ |
| **Producer Implementation** | Spring Kafka producer with acks=all, idempotence | ✅ |
| **Consumer Groups** | 3 consumer services (analytics, notification, inventory) | ✅ |
| **Schema Management** | Avro schemas with Confluent Schema Registry | ✅ |
| **Kafka Connect** | Source and sink connector configurations | ✅ |
| **Monitoring** | Prometheus + Grafana integration | ✅ |
| **Security** | SSL/TLS, SASL, ACL configuration guide | ✅ |
| **Error Handling** | Dead Letter Queue implementation | ✅ |
| **Documentation** | Architecture, runbook, security guides | ✅ |
| **Testing** | Integration tests, REST API endpoints | ✅ |

## 📝 License & Usage

This Kafka Capstone Project is designed for educational purposes and production-ready demonstrations. It showcases enterprise Kafka best practices.

## 🤝 Support & Improvements

Suggestions and improvements are welcome. This implementation serves as a reference for event-driven architectures using Apache Kafka and Spring Boot.

---

**Implementation**: Java 17 + Spring Boot 3.1.5 + Apache Kafka 3.4.0  
**Status**: Production-ready | **Version**: 1.0 | **Last Updated**: March 29, 2026
