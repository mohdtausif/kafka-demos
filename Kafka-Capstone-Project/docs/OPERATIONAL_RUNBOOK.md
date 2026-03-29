# Kafka Capstone Project - Operational Runbook

## Purpose
This runbook provides step-by-step procedures for common operational tasks, troubleshooting, and incident response.

## Table of Contents
1. [Startup Procedures](#startup-procedures)
2. [Shutdown Procedures](#shutdown-procedures)
3. [Monitoring and Alerting](#monitoring-and-alerting)
4. [Troubleshooting](#troubleshooting)
5. [Recovery Procedures](#recovery-procedures)

---

## Startup Procedures

### 1. Start Kafka Cluster
```bash
cd Kafka-Capstone-Project
docker-compose up -d

# Wait for cluster to be ready (30-60 seconds)
sleep 30

# Verify all services are running
docker-compose ps
```

Expected output: All services should be in "Up" state.

### 2. Verify Cluster Health
```bash
# Check if Kafka brokers are ready
docker exec kafka-demos-kafka1-1 kafka-broker-api-versions --bootstrap-server kafka1:29092,kafka2:29092,kafka3:29092

# Verify Zookeeper connectivity
docker exec zookeeper zkServer.sh status
```

### 3. Create Topics
```bash
chmod +x config/create-topics.sh
./config/create-topics.sh
```

Verify topics created:
```bash
docker exec kafka-demos-kafka1-1 kafka-topics --list --bootstrap-server localhost:9092,localhost:9093,localhost:9094
```

### 4. Register Avro Schemas
```bash
# Register order-event schema
curl -X POST http://localhost:8081/subjects/order-event-value/versions \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d @schemas/order-event.avsc

# Verify schemas
curl http://localhost:8081/subjects
```

### 5. Build and Start Spring Boot Application
```bash
# Terminal 1: Build the application
cd kafka-java
mvn clean package

# Start the application (consumers run automatically in background)
mvn spring-boot:run
```

### 6. Verify End-to-End Operation
```bash
# Check application is running (REST API available)
curl http://localhost:8080/api/health

# Send a test order event
curl -X POST "http://localhost:8080/api/producer/send?eventType=ORDER_CREATED"

# Check analytics metrics show the order was processed
curl http://localhost:8080/api/metrics/analytics | jq '.'

# Check inventory changes
curl http://localhost:8080/api/metrics/inventory | jq '.'

# Verify topic has data
docker exec kafka-demos-kafka1-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-events \
  --from-beginning \
  --max-messages 5
```

---

## Shutdown Procedures

### Graceful Shutdown
```bash
# 1. Stop Spring Boot application
# Press Ctrl+C in the terminal where mvn spring-boot:run is running
# This gracefully shuts down all consumer listeners

# 2. Stop Docker Compose (preserves data)
cd Kafka-Capstone-Project
docker-compose down

# This keeps volumes intact for restart
```

### Hard Shutdown (with cleanup)
```bash
# Only do this if data needs to be cleared
docker-compose down -v

# This removes containers, networks, and data volumes
```

---

## Monitoring and Alerting

### 1. Check Kafka Metrics

**Broker Health:**
```bash
# Check leader count per broker
curl http://localhost:9101/metrics | grep kafka_controller_kafkacontroller_globalpartitioncount

# Check under-replicated partitions
curl http://localhost:9101/metrics | grep kafka_server_replicamanager_underreplicatedpartitions
```

**Producer Metrics:**
```bash
# Producer record send rate
curl http://localhost:9101/metrics | grep kafka_producer_record_send_rate

# Producer record error rate
curl http://localhost:9101/metrics | grep kafka_producer_record_error_rate
```

**Consumer Metrics:**
```bash
# Consumer lag per partition
docker exec kafka-demos-kafka1-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group analytics-consumer-group \
  --describe

# Example output shows LAG column for each partition
```

### 2. Access Grafana Dashboards
```
URL: http://localhost:3000
Username: admin
Password: admin

Available Dashboards:
- Kafka Overview
- Consumer Lag
- Topic Metrics
- Broker Performance
```

### 3. Check Alert Status
```bash
# View active alerts in Prometheus
curl http://localhost:9090/api/v1/alerts | jq '.'

# View alert rules
curl http://localhost:9090/api/v1/rules | jq '.data.groups[].rules[] | select(.alert)'
```

---

## Troubleshooting

### Problem: Spring Boot Application Won't Start or Connect to Kafka

**Symptoms:**
```
Application fails to start with ConnectException
or
BrokerNotAvailableException in application logs
or
Schema Registry connection errors
```

**Diagnosis:**
```bash
# Check if Kafka brokers are running
docker ps | grep kafka

# Check if brokers are fully initialized
docker exec kafka-demos-kafka1-1 kafka-broker-api-versions --bootstrap-server kafka1:29092

# Check application logs for specific errors
# Look for: "Failed to initialize producer", "Unable to connect to Schema Registry"

# Test connectivity from host
nc -zv localhost 9092  # Kafka broker
nc -zv localhost 8081  # Schema Registry
```

**Resolution:**
```bash
# Check Docker Compose status
cd Kafka-Capstone-Project
docker-compose ps

# If services not running, restart
docker-compose restart kafka1 kafka2 kafka3 schema-registry

# Wait 30 seconds for services to stabilize
sleep 30

# Restart Spring Boot application
cd kafka-java
mvn spring-boot:run
```

### Problem: Consumer Lag Growing Continuously

**Symptoms:**
```
Lag = 1000+ and increasing
Consumer group not progressing
Spring Boot logs show slow message processing
```

**Diagnosis:**
```bash
# Check consumer group status
docker exec kafka-demos-kafka1-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group analytics-consumer-group \
  --describe

# Check if Spring Boot app is still running
curl http://localhost:8080/api/health

# Check Spring Boot logs for slowdown or exceptions
# Look for slow processing or error logs in mvn output

# Check REST API metrics
curl http://localhost:8080/api/metrics/analytics | jq '.totalOrders'
```

**Resolution:**
```bash
# Option 1: Increase consumer concurrency (Spring Boot config)
# Edit application.yml: concurrency: 10
# Then restart Spring Boot application (Ctrl+C, then mvn spring-boot:run)

# Option 2: Check processing logic for bottlenecks
# Review consumer service logs for specific slow operations

# Option 3: Reset consumer group offset to latest
docker exec kafka-demos-kafka1-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group analytics-consumer-group \
  --reset-offsets \
  --to-latest \
  --execute

# Then restart Spring Boot to resume from latest offset
```

### Problem: Broker Disk Space Full

**Symptoms:**
```
Broker crashes with "No space left on device"
Unable to write to log directory
```

**Diagnosis:**
```bash
# Check broker disk usage
docker exec kafka-demos-kafka1-1 df -h /var/lib/kafka/data

# Check topic size
docker exec kafka-demos-kafka1-1 du -sh /var/lib/kafka/data/*

# Check log retention settings
docker exec kafka-demos-kafka1-1 kafka-configs --bootstrap-server localhost:9092 \
  --describe --entity-type topics --entity-name order-events
```

**Resolution:**
```bash
# Option 1: Increase log retention
docker exec kafka-demos-kafka1-1 kafka-configs --bootstrap-server localhost:9092 \
  --alter --entity-type topics --entity-name order-events \
  --add-config retention.bytes=1073741824

# Option 2: Delete old topics
docker exec kafka-demos-kafka1-1 kafka-topics --delete \
  --topic old-topic-name \
  --bootstrap-server localhost:9092

# Option 3: Expand storage volume
# This requires Docker volume resizing and may need downtime
```

### Problem: Schema Registry Errors

**Symptoms:**
```
ERROR: Unable to find schema for topic=order-events
or
Schema serialization errors
```

**Diagnosis:**
```bash
# Check schema registry health
curl http://localhost:8081/subjects

# Check if schemas are registered
curl http://localhost:8081/subjects/order-event-value/versions

# Verify schema validity
curl -X POST http://localhost:8081/compatibility/subjects/order-event-value/versions/latest \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d @schemas/order-event.avsc
```

**Resolution:**
```bash
# Re-register schema
curl -X POST http://localhost:8081/subjects/order-event-value/versions \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d @schemas/order-event.avsc

# Check schema compatibility
# Update schema if needed and register new version
```

---

## Recovery Procedures

### 1. Broker Failure Recovery

**When a Broker Goes Down:**
```bash
# Monitor topic replication status
docker exec kafka-demos-kafka1-1 kafka-topics --describe \
  --topic order-events \
  --under-replicated-partitions \
  --bootstrap-server localhost:9092

# ISR (In-Sync Replicas) will shrink temporarily
```

**Recovery Steps:**
```bash
# Wait for broker to come back online
docker-compose restart kafka2

# Monitor recovery progress
watch -n 5 'docker exec kafka-demos-kafka1-1 kafka-topics --describe --topic order-events --bootstrap-server localhost:9092'

# Once ISR is restored (Replicas == ISR), broker is healthy
```

### 2. Consumer Group Offset Reset (Last Resort)

**Scenario:** Consumer processed incorrect messages or data corruption

```bash
# Option A: Reset to earliest (reprocess all messages)
docker exec kafka-demos-kafka1-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group analytics-consumer-group \
  --reset-offsets \
  --to-earliest \
  --execute

# Option B: Reset to latest (skip backlog)
docker exec kafka-demos-kafka1-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group analytics-consumer-group \
  --reset-offsets \
  --to-latest \
  --execute

# Option C: Reset to specific timestamp
docker exec kafka-demos-kafka1-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group analytics-consumer-group \
  --reset-offsets \
  --to-datetime 2024-01-01T00:00:00.000 \
  --execute
```

### 3. Message Replay from Dead Letter Queue

**Scenario:** Messages failed and need reprocessing

```bash
# Read messages from DLQ
docker exec kafka-demos-kafka1-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-events-dlq \
  --from-beginning

# Move back to main topic (if safe)
# Create a producer script to read from DLQ and write to main topic
```

### 4. Full Cluster Recovery

**If multiple brokers fail or data is corrupted:**

```bash
# 1. Stop everything
docker-compose down

# 2. Backup current data (optional)
docker volume ls | grep kafka
# Manual backup if needed

# 3. Remove volumes and start fresh
docker-compose down -v
rm -rf docker/volumes/*

# 4. Start cluster
docker-compose up -d

# 5. Recreate topics
./config/create-topics.sh

# 6. Re-register schemas
# See Startup Procedures step 4

# 7. Restart applications
# See Startup Procedures step 5
```

---

## Spring Boot Metrics & Monitoring

### REST API Endpoints
```bash
# Application health
curl http://localhost:8080/api/health

# Send messages for testing
curl -X POST "http://localhost:8080/api/producer/send?eventType=ORDER_CREATED"
curl -X POST "http://localhost:8080/api/producer/send-batch?count=100"
curl -X POST "http://localhost:8080/api/producer/send-lifecycle"

# Get metrics
curl http://localhost:8080/api/metrics/analytics | jq '.'
curl http://localhost:8080/api/metrics/inventory | jq '.'
curl "http://localhost:8080/api/metrics/inventory/PROD_0001" | jq '.'
curl http://localhost:8080/api/metrics/notifications | jq '.'
```

### Application Logs
```bash
# View Spring Boot logs (showing consumer messages)
# Logs appear directly in mvn spring-boot:run terminal output
# Look for:
# - "Processed X order events" from AnalyticsConsumerService
# - "Sending notification" from NotificationConsumerService
# - "Inventory reserved/deducted" from InventoryConsumerService
```

---

## Maintenance Schedule

### Daily
- Monitor consumer lag via REST API (should be < 100 messages)
- Check REST API health endpoint: `curl http://localhost:8080/api/health`
- Verify Spring Boot application is running
- Check broker disk usage (should be < 80%)

### Weekly
- Review Spring Boot application logs for errors
- Check Grafana dashboards for trends
- Check under-replicated partitions count
- Analyze REST API metrics for processing trends

### Monthly
- Full backup of Kafka cluster (snapshot volumes)
- Review Spring Boot configuration for optimization opportunities
- Review and archive old topics
- Capacity planning review

### Quarterly
- Kafka cluster upgrade testing
- Security audit and ACL review
- Performance baseline comparison

---

## Escalation Contacts

- **Platform Owner**: Name (ext)
- **Kafka Operator**: Name (ext)
- **On-Call**: PagerDuty [link]
