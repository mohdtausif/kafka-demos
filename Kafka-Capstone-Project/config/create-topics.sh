#!/bin/bash
# Script to create Kafka topics with proper configuration

BROKERS="localhost:9092,localhost:9093,localhost:9094"
REPLICATION_FACTOR=2
PARTITIONS=3

echo "Creating Kafka topics for Capstone Project..."

# Order events topic
docker exec kafka-demos-kafka1-1 kafka-topics --create \
  --bootstrap-server $BROKERS \
  --topic order-events \
  --partitions $PARTITIONS \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=604800000 \
  --config compression.type=snappy \
  2>/dev/null || echo "✓ order-events topic already exists"

# Order analytics topic (output from Kafka Streams)
docker exec kafka-demos-kafka1-1 kafka-topics --create \
  --bootstrap-server $BROKERS \
  --topic order-analytics \
  --partitions $PARTITIONS \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=2592000000 \
  2>/dev/null || echo "✓ order-analytics topic already exists"

# Dead Letter Queue topic
docker exec kafka-demos-kafka1-1 kafka-topics --create \
  --bootstrap-server $BROKERS \
  --topic order-events-dlq \
  --partitions 1 \
  --replication-factor $REPLICATION_FACTOR \
  2>/dev/null || echo "✓ order-events-dlq topic already exists"

# Customer data topic (for KTable)
docker exec kafka-demos-kafka1-1 kafka-topics --create \
  --bootstrap-server $BROKERS \
  --topic customers \
  --partitions $PARTITIONS \
  --replication-factor $REPLICATION_FACTOR \
  --config cleanup.policy=compact \
  2>/dev/null || echo "✓ customers topic already exists"

echo "Topic creation complete!"

# List all topics
echo -e "\n=== Created Topics ==="
docker exec kafka-demos-kafka1-1 kafka-topics --list --bootstrap-server $BROKERS
