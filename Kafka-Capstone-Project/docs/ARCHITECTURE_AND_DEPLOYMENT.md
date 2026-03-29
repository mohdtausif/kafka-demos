# Kafka Capstone Project - Architecture & Deployment Guide

## Project Overview

This Kafka Capstone Project implements a complete event-driven data pipeline for an e-commerce platform using Apache Kafka. The system processes order lifecycle events in real-time with multiple consumers, Kafka Streams analytics, Kafka Connect integrations, and comprehensive monitoring.

## Architecture Components

### 1. **Kafka Cluster (3 Brokers)**
- Multi-broker setup with Zookeeper for high availability
- Replication factor: 2 for fault tolerance
- Topics: `order-events`, `order-analytics`, `customers`, `order-events-dlq`

### 2. **Schema Registry**
- Avro schemas for type-safe serialization
- Schemas: `order-event.avsc`, `customer.avsc`
- Enables schema evolution and compatibility checking

### 3. **Producer Application**
- `OrderProducerService.java`: Spring Boot reliable order event producer
- Configuration: acks=all, idempotent, compression=snappy
- Generates order lifecycle events (ORDER_CREATED, PAYMENT_PROCESSED, ORDER_SHIPPED, ORDER_DELIVERED)
- Partitioning strategy: partition by order_id for ordering guarantees
- REST API: POST `/api/producer/send` with event type parameter

### 4. **Consumer Applications (3 consumers)**
- **AnalyticsConsumerService**: Aggregates order metrics (count, revenue, top customers)
- **NotificationConsumerService**: Sends notifications for each order event with DLQ support
- **InventoryConsumerService**: Updates inventory reserves and stock based on order events

### 5. **Kafka Connect**
- Distributed mode deployment
- File Source Connector: Reads customer data
- File Sink Connector: Writes processed events

### 6. **Monitoring & Observability**
- Prometheus: Scrapes JMX metrics from brokers
- Grafana: Visualizes metrics and dashboards
- Alert rules for critical conditions

## Quick Start

### Prerequisites
- Docker and Docker Compose
- Java 17 or higher
- Maven 3.8 or higher

### 1. Start the Kafka Cluster
```bash
cd Kafka-Capstone-Project
docker-compose up -d
# Wait 10 seconds for cluster to start
sleep 10
```

### 2. Create Topics
```bash
chmod +x config/create-topics.sh
./config/create-topics.sh
```

### 3. Build the Spring Boot Application
```bash
cd kafka-java
mvn clean package
```

### 4. Register Avro Schemas
```bash
# Register order-event schema
curl -X POST http://localhost:8081/subjects/order-event-value/versions \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d @../schemas/order-event.avsc

# Verify schema registered
curl http://localhost:8081/subjects
```

### 5. Run Spring Boot Application
```bash
# Terminal 1: Start the Spring Boot application
mvn spring-boot:run

# The application starts with:
# - Producer service ready
# - 3 consumer services running in background
# - REST API on http://localhost:8080
```

### 6. Verify End-to-End Operation
```bash
# Send a test order event via REST API
curl -X POST "http://localhost:8080/api/producer/send?eventType=ORDER_CREATED"

# Check analytics metrics
curl http://localhost:8080/api/metrics/analytics | jq '.'

# Check inventory status
curl http://localhost:8080/api/metrics/inventory | jq '.'

# Check notification metrics
curl http://localhost:8080/api/metrics/notifications | jq '.'
```

## Deployment Architecture

```
┌─────────────────────────────────────────────────┐
│           Order Event Producer                   │
│      (order_producer.py)                         │
│  - Reliable config (acks=all)                    │
│  - Partitioned by order_id                       │
│  - Avro serialization                            │
└──────────────────┬──────────────────────────────┘
                   │ Publishes to:
                   ▼
         ┌─────────────────────┐
         │  Order Events Topic  │
         │  (3 partitions,     │
         │   RF=2)             │
         └────────────┬────────┘
                      │
        ┌─────────────┼──────────────┐
        │             │              │
        ▼             ▼              ▼
   ┌─────────┐ ┌──────────┐ ┌──────────────┐
   │Analytics│ │Notification│ │  Inventory  │
   │Consumer │ │ Consumer  │ │  Consumer   │
   │         │ │          │ │             │
   │- Counts │ │- Sends   │ │- Reserves   │
   │- Revenue│ │  notifs  │ │  inventory  │
   │- Top 5  │ │- DLQ     │ │- Deducts    │
   │  cust.  │ │  support │ │  on ship    │
   └────┬────┘ └────┬─────┘ └──────┬──────┘
        │           │              │
        ▼           ▼              ▼
  ┌──────────┐  ┌──────┐  ┌──────────────┐
  │ Metrics  │  │ DLQ  │  │  Inventory   │
  │          │  │      │  │  Updates     │
  └──────────┘  └──────┘  └──────────────┘

Monitoring Layer:
┌─────────────────────────────────┐
│  JMX Metrics from Brokers       │
└────────────┬────────────────────┘
             │
             ▼
      ┌────────────────┐
      │  Prometheus    │
      │  (collection)  │
      └────────┬───────┘
               │
               ▼
      ┌────────────────┐
      │   Grafana      │
      │  (dashboards)  │
      └────────────────┘
```

## Configuration Details

### Producer Configuration
- `bootstrap.servers`: Multi-broker list for failover
- `acks`: 'all' - Wait for all in-sync replicas
- `retries`: 3 - Automatic retry on failure
- `compression.type`: 'snappy' - Compress messages
- `max.in.flight.requests.per.connection`: 1 - Preserve ordering

### Consumer Configuration
- `auto.offset.reset`: 'earliest' - Start from beginning
- `enable.auto.commit`: False - Manual offset management
- `session.timeout.ms`: 60000 - Heartbeat timeout
- Consumer groups: analytics-consumer-group, notification-consumer-group, inventory-consumer-group

### Topic Configuration
- `order-events`: 3 partitions, RF=2, 7-day retention
- `order-analytics`: 3 partitions, RF=2, 30-day retention
- `customers`: 3 partitions, RF=2, log-compact cleanup
- `order-events-dlq`: 1 partition, RF=2, DLQ handling

## Operational Commands

### Topic Management
```bash
# List topics
docker exec kafka-demos-kafka1-1 kafka-topics --list --bootstrap-server localhost:9092,localhost:9093,localhost:9094

# Describe topic
docker exec kafka-demos-kafka1-1 kafka-topics --describe --topic order-events --bootstrap-server localhost:9092,localhost:9093,localhost:9094

# Consumer group status
docker exec kafka-demos-kafka1-1 kafka-consumer-groups --bootstrap-server localhost:9092,localhost:9093,localhost:9094 --group analytics-consumer-group --describe
```

### Monitoring
```bash
# Check Kafka broker metrics
curl http://localhost:9101/metrics | grep kafka

# View Prometheus targets
curl http://localhost:9090/api/v1/targets

# Query metrics
curl 'http://localhost:9090/api/v1/query?query=kafka_server_replicafetcher_max_lag'
```

## Error Handling & Resilience

1. **Dead Letter Queue (DLQ)**
   - Topic: `order-events-dlq`
   - Failed messages from consumers routed here
   - Enables debugging and replay

2. **Retry Logic**
   - Producer: 3 automatic retries with exponential backoff
   - Consumer: Manual error handling and offset management

3. **Fault Tolerance**
   - Broker replication factor: 2
   - IminISR: 1 (allows 1 broker failure)
   - Consumer group rebalancing on broker failures

## Security Considerations

For production deployment, implement:
1. **SSL/TLS**: Encrypt communication between brokers and clients
2. **SASL Authentication**: User authentication (PLAIN or SCRAM)
3. **ACLs**: Role-based access control on topics
4. **Client Quotas**: Prevent resource exhaustion

## Performance Tuning

1. **Producer**: Batch messages, tune linger.ms and batch.size
2. **Consumer**: Configure fetch.min.bytes, fetch.max.wait.ms
3. **Broker**: Tune num.network.threads, num.io.threads
4. **Replication**: Monitor ISR (In-Sync Replicas) and unclean leader election

## Troubleshooting

### Consumer not receiving messages
- Check topic partition count: `kafka-topics --describe`
- Check consumer group status: `kafka-consumer-groups --describe`
- Verify network connectivity to brokers

### High consumer lag
- Check consumer throughput: Monitor message processing rate
- Increase consumer instances for parallel processing
- Check for slow processing logic in consumer

### Broker down
- Check Docker container status: `docker ps`
- Review broker logs: `docker logs kafka-demos-kafka1-1`
- Check Zookeeper connectivity: `zookeeper-shell`

### Schema Registry issues
- Verify schema compatibility
- Check topic naming convention (matches schema name)
- Review Avro schema syntax

## Scaling Considerations

1. **Horizontal Scaling**
   - Add more consumer instances to increase throughput
   - Partitions should match or exceed consumer count

2. **Vertical Scaling**
   - Increase broker resources (CPU, memory)
   - Increase producer batch size
   - Increase consumer fetch size

3. **Kafka Streams Application**
   - Deploy on multiple instances for parallel processing
   - Configure state directory for changelog topics

## References

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Confluent Platform](https://docs.confluent.io/)
- [Kafka Schema Registry](https://docs.confluent.io/platform/current/schema-registry/)
- [Best Practices](https://kafka.apache.org/documentation/#bestpractices)
