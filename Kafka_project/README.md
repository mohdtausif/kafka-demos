# EcommerceFlow — Kafka Producer-Consumer Pipeline

An event-driven microservices architecture for real-time e-commerce order processing using Apache Kafka, Spring Boot, Avro/Schema Registry, Kafka Streams, Kafka Connect, Prometheus/Grafana monitoring, and SSL/SASL security.

## 🏗️ Architecture Overview

```
┌──────────────┐     ┌────────────────────────────────────────────────────────┐
│  REST Client  │────▶│              Order Producer (port 8080)               │
└──────────────┘     │  POST /api/orders, PUT /pay, /ship, /deliver          │
                     └────────────────────┬───────────────────────────────────┘
                                          │ Kafka Events (Avro + Schema Registry)
                     ┌────────────────────▼───────────────────────────────────┐
                     │              Apache Kafka Cluster (3 Brokers)          │
                     │                                                        │
                     │  Topics: order.created, order.paid, order.shipped,     │
                     │          order.delivered, order.dlq                     │
                     │  Analytics: order.analytics.counts, .revenue           │
                     │  Connectors: customers, notifications                  │
                     └──┬──────────┬──────────┬──────────┬───────────────────┘
                        │          │          │          │
              ┌─────────▼──┐ ┌────▼──────┐ ┌─▼────────┐ ┌▼──────────────────┐
              │  Analytics  │ │Notification│ │Inventory │ │  Kafka Streams    │
              │  Consumer   │ │ Consumer   │ │ Consumer │ │  Analytics        │
              │ (port 8082) │ │(port 8084) │ │(port 8086)│ │ (port 8088)      │
              └─────────────┘ └───────────┘ └──────────┘ └───────────────────┘
                        │                                         │
              ┌─────────▼──────────────┐     ┌───────────────────▼────────────┐
              │  GET /api/analytics     │     │  Tumbling Window Aggregations  │
              │  GET /api/inventory     │     │  KStream-KTable Join           │
              └────────────────────────┘     └────────────────────────────────┘

   ┌─────────────────────┐    ┌──────────────────────────────────────────────┐
   │     PostgreSQL       │◀──│     Kafka Connect (JDBC Source + File Sink)  │
   │   (customers, inv)   │──▶│     Distributed Mode (port 8083)             │
   └─────────────────────┘    └──────────────────────────────────────────────┘

   ┌─────────────────────┐    ┌──────────────────────────────────────────────┐
   │  Prometheus (9090)   │◀──│     Kafka JMX Metrics + Actuator Endpoints   │
   │  Grafana    (3000)   │    │                                              │
   └─────────────────────┘    └──────────────────────────────────────────────┘
```

## 📦 Project Modules

| Module | Description | Port |
|--------|-------------|------|
| `common` | Shared Avro schemas, topic constants, enums | N/A |
| `order-producer` | REST API to publish order lifecycle events | 8080 |
| `analytics-consumer` | Order metrics aggregation (counts, revenue) | 8082 |
| `notification-consumer` | Simulated email/SMS notifications | 8084 |
| `inventory-consumer` | Stock reservation and deduction | 8086 |
| `kafka-streams-analytics` | Real-time windowed aggregations, KStream-KTable join | 8088 |

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose

### 1. Start Infrastructure
```bash
docker compose up -d
```
This starts: 3 Kafka brokers (KRaft), Schema Registry, Kafka Connect, PostgreSQL, Prometheus, and Grafana.

### 2. Build All Services
```bash
mvn clean package -DskipTests
```

### 3. Run Services (in separate terminals)
```bash
# Producer
java -jar order-producer/target/order-producer-1.0.0-SNAPSHOT.jar

# Consumers
java -jar analytics-consumer/target/analytics-consumer-1.0.0-SNAPSHOT.jar
java -jar notification-consumer/target/notification-consumer-1.0.0-SNAPSHOT.jar
java -jar inventory-consumer/target/inventory-consumer-1.0.0-SNAPSHOT.jar

# Kafka Streams
java -jar kafka-streams-analytics/target/kafka-streams-analytics-1.0.0-SNAPSHOT.jar
```

### 4. Test the Pipeline

**Create an order:**
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "C001",
    "productId": "P001",
    "quantity": 2,
    "price": 79.99,
    "shippingAddress": "123 Main St, New York, NY"
  }'
```

**Process payment:**
```bash
curl -X PUT http://localhost:8080/api/orders/{orderId}/pay \
  -H "Content-Type: application/json" \
  -d '{"paymentMethod": "CREDIT_CARD"}'
```

**Ship the order:**
```bash
curl -X PUT http://localhost:8080/api/orders/{orderId}/ship \
  -H "Content-Type: application/json" \
  -d '{"carrier": "FEDEX", "shippingAddress": "123 Main St"}'
```

**Mark as delivered:**
```bash
curl -X PUT http://localhost:8080/api/orders/{orderId}/deliver \
  -H "Content-Type: application/json" \
  -d '{"deliveredTo": "John Doe", "signatureRequired": true}'
```

### 5. Check Results
- **Analytics:** `curl http://localhost:8082/api/analytics`
- **Inventory:** `curl http://localhost:8086/api/inventory`
- **Schema Registry:** `curl http://localhost:8081/subjects`
- **Grafana Dashboard:** `http://localhost:3000` (admin/admin)
- **Prometheus:** `http://localhost:9090`

### 6. Deploy Kafka Connect Connectors
```bash
# JDBC Source (customers table → Kafka)
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @connect/jdbc-source-connector.json

# File Sink (notifications → file)
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @connect/file-sink-connector.json
```

## 🔐 Security

Security configurations are provided in the `security/` directory:
- **SSL/TLS:** Run `security/generate-certs.sh` to generate certificates
- **SASL/SCRAM:** JAAS configs in `security/kafka_server_jaas.conf` and `security/kafka_client_jaas.conf`
- See `docs/operational-runbook.md` for enabling security in production

## 📊 Monitoring

- **Prometheus** scrapes Kafka JMX metrics and Spring Boot Actuator endpoints
- **Grafana** has pre-provisioned dashboards for broker metrics, consumer lag, and throughput
- Alert rules can be configured for high consumer lag or broker failures

## 📁 Project Structure
```
├── pom.xml                          # Parent POM
├── docker-compose.yml               # Full infrastructure stack
├── common/                          # Shared Avro schemas & constants
├── order-producer/                  # REST → Kafka producer
├── analytics-consumer/              # Metrics aggregation consumer
├── notification-consumer/           # Notification consumer
├── inventory-consumer/              # Inventory management consumer
├── kafka-streams-analytics/         # Real-time stream processing
├── connect/                         # Kafka Connect connector configs
├── monitoring/                      # Prometheus, Grafana, JMX configs
├── security/                        # SSL/TLS & SASL/SCRAM configs
├── scripts/                         # Init scripts (DB, topics)
└── docs/                            # Architecture & operational docs
```
