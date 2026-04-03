# Operational Runbook

## Common Operations

### Listing Topics
```bash
docker exec kafka-1 kafka-topics --bootstrap-server kafka-1:29092 --list
```

### Describing a Topic
```bash
docker exec kafka-1 kafka-topics --bootstrap-server kafka-1:29092 --describe --topic order.created
```

### Viewing Consumer Group Lag
```bash
docker exec kafka-1 kafka-consumer-groups --bootstrap-server kafka-1:29092 \
  --group analytics-group --describe
```

### Reading Messages from a Topic
```bash
docker exec kafka-1 kafka-console-consumer --bootstrap-server kafka-1:29092 \
  --topic order.created --from-beginning --max-messages 10
```

### Checking DLQ
```bash
docker exec kafka-1 kafka-console-consumer --bootstrap-server kafka-1:29092 \
  --topic order.dlq --from-beginning --max-messages 10
```

---

## Troubleshooting

### High Consumer Lag
1. Check consumer group status: `kafka-consumer-groups --describe --group <group-id>`
2. Check consumer logs for errors
3. Scale consumers (add instances to the consumer group)
4. Check if a partition is stuck (specific partition lag increasing)

### Broker Down
1. Check broker logs: `docker compose logs kafka-1`
2. Verify cluster health: check under-replicated partitions in Grafana
3. Restart the broker: `docker compose restart kafka-1`
4. Verify recovery: check leader election and ISR status

### Schema Registry Issues
1. Check registry health: `curl http://localhost:8081/`
2. List subjects: `curl http://localhost:8081/subjects`
3. Check compatibility: `curl http://localhost:8081/config`
4. Restart: `docker compose restart schema-registry`

### Connector Failures
1. Check status: `curl http://localhost:8083/connectors/<name>/status`
2. Restart task: `curl -X POST http://localhost:8083/connectors/<name>/tasks/0/restart`
3. Check Connect logs: `docker compose logs kafka-connect`
4. Delete and recreate: `curl -X DELETE http://localhost:8083/connectors/<name>`

### Message Serialization Errors
1. Check Schema Registry connectivity
2. Verify schema compatibility
3. Check DLQ topic for failed messages
4. Review consumer deserialization config (`specific.avro.reader=true`)

---

## Scaling

### Adding Consumers to a Group
Simply start another instance with the same `group-id`. Kafka will automatically rebalance partitions.

### Increasing Topic Partitions
```bash
docker exec kafka-1 kafka-topics --bootstrap-server kafka-1:29092 \
  --alter --topic order.created --partitions 12
```
> ⚠️ Increasing partitions can break key-based ordering guarantees for existing keys.

### Adding a Kafka Broker
1. Add new broker service to `docker-compose.yml`
2. Start: `docker compose up -d kafka-4`
3. Reassign partitions: use `kafka-reassign-partitions` tool

---

## Failure Recovery

### Replaying Messages
Consumers can replay messages by resetting offsets:
```bash
docker exec kafka-1 kafka-consumer-groups --bootstrap-server kafka-1:29092 \
  --group analytics-group --reset-offsets --to-earliest --topic order.created --execute
```

### Recovering from DLQ
1. Read failed messages from `order.dlq`
2. Fix the root cause (data issue, bug, etc.)
3. Re-publish corrected messages to the original topic

---

## Security Operations

### Generating New SSL Certificates
```bash
cd security && bash generate-certs.sh
```

### Creating ACL Policies
```bash
# Allow producer user to write to order topics
docker exec kafka-1 kafka-acls --bootstrap-server kafka-1:29092 \
  --add --allow-principal User:producer \
  --operation Write --topic order.created

# Allow consumer user to read from order topics
docker exec kafka-1 kafka-acls --bootstrap-server kafka-1:29092 \
  --add --allow-principal User:consumer \
  --operation Read --topic order.created --group analytics-group
```

### Setting Client Quotas
```bash
docker exec kafka-1 kafka-configs --bootstrap-server kafka-1:29092 \
  --alter --add-config 'producer_byte_rate=1048576,consumer_byte_rate=2097152' \
  --entity-type users --entity-name producer
```
