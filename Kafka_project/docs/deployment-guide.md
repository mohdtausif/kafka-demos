# Deployment Guide

## Prerequisites

- Java 17 or higher
- Apache Maven 3.8+
- Docker & Docker Compose v2
- 8GB+ RAM recommended (Kafka brokers are memory-intensive)

## Step 1: Start Infrastructure

```bash
cd /path/to/Kafka_project
docker compose up -d
```

Verify all services are running:
```bash
docker compose ps
```

Expected services: kafka-1, kafka-2, kafka-3, schema-registry, kafka-connect, postgres, prometheus, grafana, kafka-init.

Wait for topic initialization (kafka-init container will exit after creating topics):
```bash
docker compose logs kafka-init
```

## Step 2: Build All Modules

```bash
mvn clean package -DskipTests
```

This compiles all modules, generates Avro classes from schemas, and produces executable JARs.

## Step 3: Run Microservices

Start each service in a separate terminal:

```bash
# Order Producer (port 8080)
java -jar order-producer/target/order-producer-1.0.0-SNAPSHOT.jar

# Analytics Consumer (port 8082)
java -jar analytics-consumer/target/analytics-consumer-1.0.0-SNAPSHOT.jar

# Notification Consumer (port 8084)
java -jar notification-consumer/target/notification-consumer-1.0.0-SNAPSHOT.jar

# Inventory Consumer (port 8086)
java -jar inventory-consumer/target/inventory-consumer-1.0.0-SNAPSHOT.jar

# Kafka Streams Analytics (port 8088)
java -jar kafka-streams-analytics/target/kafka-streams-analytics-1.0.0-SNAPSHOT.jar
```

## Step 4: Deploy Kafka Connect Connectors

Wait for Kafka Connect to be fully ready:
```bash
curl http://localhost:8083/connectors
```

Deploy connectors:
```bash
# JDBC Source Connector (PostgreSQL → Kafka)
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @connect/jdbc-source-connector.json

# File Sink Connector (Kafka → File)
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @connect/file-sink-connector.json
```

Verify connector status:
```bash
curl http://localhost:8083/connectors/jdbc-source-connector/status
curl http://localhost:8083/connectors/file-sink-connector/status
```

## Step 5: Verify the System

### Check Schema Registry
```bash
curl http://localhost:8081/subjects
```

### Check Topics
```bash
docker exec kafka-1 kafka-topics --bootstrap-server kafka-1:29092 --list
```

### Check Consumer Groups
```bash
docker exec kafka-1 kafka-consumer-groups --bootstrap-server kafka-1:29092 --list
```

### Access Monitoring
- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090

## Configuration Reference

| Service | Port | Config File |
|---------|------|-------------|
| Order Producer | 8080 | order-producer/src/main/resources/application.yml |
| Analytics Consumer | 8082 | analytics-consumer/src/main/resources/application.yml |
| Notification Consumer | 8084 | notification-consumer/src/main/resources/application.yml |
| Inventory Consumer | 8086 | inventory-consumer/src/main/resources/application.yml |
| Kafka Streams | 8088 | kafka-streams-analytics/src/main/resources/application.yml |
| Schema Registry | 8081 | docker-compose.yml |
| Kafka Connect | 8083 | docker-compose.yml |
| Prometheus | 9090 | monitoring/prometheus.yml |
| Grafana | 3000 | monitoring/grafana/ |

## Stopping Everything

```bash
# Stop microservices (Ctrl+C in each terminal)

# Stop infrastructure
docker compose down

# Stop and remove all data
docker compose down -v
```
