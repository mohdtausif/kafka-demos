# Access Control Lists (ACL) & Security Policies

## Authentication Setup

### SASL/SCRAM Configuration

EcommerceFlow uses SASL/SCRAM-SHA-256 for client authentication. Users are created in the Kafka cluster with the following roles:

| User | Role | Description |
|------|------|-------------|
| `admin` | Super admin | Full cluster access |
| `producer` | Order producer | Write access to order topics |
| `analytics-consumer` | Analytics service | Read access to order topics |
| `notification-consumer` | Notification service | Read access to order topics |
| `inventory-consumer` | Inventory service | Read access to order.created, order.shipped |
| `streams-app` | Kafka Streams | Read/Write for streams processing |
| `connect-worker` | Kafka Connect | Read/Write for connectors |

### Creating SCRAM Users
```bash
# Create users in the Kafka cluster
docker exec kafka-1 kafka-configs --bootstrap-server kafka-1:29092 \
  --alter --add-config 'SCRAM-SHA-256=[iterations=4096,password=producer-secret]' \
  --entity-type users --entity-name producer

docker exec kafka-1 kafka-configs --bootstrap-server kafka-1:29092 \
  --alter --add-config 'SCRAM-SHA-256=[iterations=4096,password=analytics-secret]' \
  --entity-type users --entity-name analytics-consumer

docker exec kafka-1 kafka-configs --bootstrap-server kafka-1:29092 \
  --alter --add-config 'SCRAM-SHA-256=[iterations=4096,password=notification-secret]' \
  --entity-type users --entity-name notification-consumer

docker exec kafka-1 kafka-configs --bootstrap-server kafka-1:29092 \
  --alter --add-config 'SCRAM-SHA-256=[iterations=4096,password=inventory-secret]' \
  --entity-type users --entity-name inventory-consumer

docker exec kafka-1 kafka-configs --bootstrap-server kafka-1:29092 \
  --alter --add-config 'SCRAM-SHA-256=[iterations=4096,password=streams-secret]' \
  --entity-type users --entity-name streams-app
```

## ACL Policies

### Producer ACLs
```bash
# Allow producer to write to order lifecycle topics
for topic in order.created order.paid order.shipped order.delivered; do
  docker exec kafka-1 kafka-acls --bootstrap-server kafka-1:29092 \
    --add --allow-principal User:producer \
    --operation Write --operation Describe \
    --topic $topic
done
```

### Analytics Consumer ACLs
```bash
# Allow analytics-consumer to read all order topics
for topic in order.created order.paid order.shipped order.delivered order.dlq; do
  docker exec kafka-1 kafka-acls --bootstrap-server kafka-1:29092 \
    --add --allow-principal User:analytics-consumer \
    --operation Read --operation Describe \
    --topic $topic
done

# Allow consumer group access
docker exec kafka-1 kafka-acls --bootstrap-server kafka-1:29092 \
  --add --allow-principal User:analytics-consumer \
  --operation Read --group analytics-group
```

### Notification Consumer ACLs
```bash
for topic in order.created order.paid order.shipped order.delivered order.dlq; do
  docker exec kafka-1 kafka-acls --bootstrap-server kafka-1:29092 \
    --add --allow-principal User:notification-consumer \
    --operation Read --operation Describe \
    --topic $topic
done

docker exec kafka-1 kafka-acls --bootstrap-server kafka-1:29092 \
  --add --allow-principal User:notification-consumer \
  --operation Read --group notification-group
```

### Inventory Consumer ACLs
```bash
for topic in order.created order.shipped order.dlq; do
  docker exec kafka-1 kafka-acls --bootstrap-server kafka-1:29092 \
    --add --allow-principal User:inventory-consumer \
    --operation Read --operation Describe \
    --topic $topic
done

docker exec kafka-1 kafka-acls --bootstrap-server kafka-1:29092 \
  --add --allow-principal User:inventory-consumer \
  --operation Read --group inventory-group
```

### Kafka Streams ACLs
```bash
# Streams needs read on input topics and write on output topics
for topic in order.created order.paid customers; do
  docker exec kafka-1 kafka-acls --bootstrap-server kafka-1:29092 \
    --add --allow-principal User:streams-app \
    --operation Read --operation Describe \
    --topic $topic
done

for topic in order.analytics.counts order.analytics.revenue; do
  docker exec kafka-1 kafka-acls --bootstrap-server kafka-1:29092 \
    --add --allow-principal User:streams-app \
    --operation Write --operation Describe \
    --topic $topic
done

# Streams internal topics (prefixed with application-id)
docker exec kafka-1 kafka-acls --bootstrap-server kafka-1:29092 \
  --add --allow-principal User:streams-app \
  --operation All --topic ecommerceflow-streams-analytics --resource-pattern-type prefixed
```

### DLQ Write ACLs (for all consumers)
```bash
for user in analytics-consumer notification-consumer inventory-consumer; do
  docker exec kafka-1 kafka-acls --bootstrap-server kafka-1:29092 \
    --add --allow-principal User:$user \
    --operation Write \
    --topic order.dlq
done
```

## Client Quotas

Prevent resource exhaustion by setting producer/consumer byte rate limits:

```bash
# Producer quota: max 1 MB/s write
docker exec kafka-1 kafka-configs --bootstrap-server kafka-1:29092 \
  --alter --add-config 'producer_byte_rate=1048576' \
  --entity-type users --entity-name producer

# Consumer quotas: max 2 MB/s read per consumer
for user in analytics-consumer notification-consumer inventory-consumer streams-app; do
  docker exec kafka-1 kafka-configs --bootstrap-server kafka-1:29092 \
    --alter --add-config 'consumer_byte_rate=2097152' \
    --entity-type users --entity-name $user
done

# Request percentage quota (CPU time)
docker exec kafka-1 kafka-configs --bootstrap-server kafka-1:29092 \
  --alter --add-config 'request_percentage=25' \
  --entity-type users --entity-name producer
```

## Listing ACLs
```bash
docker exec kafka-1 kafka-acls --bootstrap-server kafka-1:29092 --list
```

## SSL/TLS Configuration

See `security/generate-certs.sh` for certificate generation. SSL is configured for:
- **Broker-to-broker** communication (inter-broker listener)
- **Client-to-broker** communication (external listener)

Key files:
- `security/kafka.server.keystore.jks` — Broker keystores
- `security/kafka.server.truststore.jks` — Broker truststores
- `security/kafka.client.keystore.jks` — Client keystore
- `security/kafka.client.truststore.jks` — Client truststore
