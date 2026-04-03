#!/bin/bash
# ============================================
# Create Kafka Topics for EcommerceFlow
# ============================================

BOOTSTRAP_SERVER=${1:-localhost:9092}

echo "Creating Kafka topics on $BOOTSTRAP_SERVER..."

# Order lifecycle topics (6 partitions, replication factor 3)
kafka-topics --bootstrap-server "$BOOTSTRAP_SERVER" --create --if-not-exists --topic order.created --partitions 6 --replication-factor 3
kafka-topics --bootstrap-server "$BOOTSTRAP_SERVER" --create --if-not-exists --topic order.paid --partitions 6 --replication-factor 3
kafka-topics --bootstrap-server "$BOOTSTRAP_SERVER" --create --if-not-exists --topic order.shipped --partitions 6 --replication-factor 3
kafka-topics --bootstrap-server "$BOOTSTRAP_SERVER" --create --if-not-exists --topic order.delivered --partitions 6 --replication-factor 3

# Dead letter queue
kafka-topics --bootstrap-server "$BOOTSTRAP_SERVER" --create --if-not-exists --topic order.dlq --partitions 3 --replication-factor 3

# Analytics output topics
kafka-topics --bootstrap-server "$BOOTSTRAP_SERVER" --create --if-not-exists --topic order.analytics.counts --partitions 3 --replication-factor 3
kafka-topics --bootstrap-server "$BOOTSTRAP_SERVER" --create --if-not-exists --topic order.analytics.revenue --partitions 3 --replication-factor 3

# Connector topics
kafka-topics --bootstrap-server "$BOOTSTRAP_SERVER" --create --if-not-exists --topic customers --partitions 3 --replication-factor 3
kafka-topics --bootstrap-server "$BOOTSTRAP_SERVER" --create --if-not-exists --topic notifications --partitions 3 --replication-factor 3

echo ""
echo "All topics created. Listing topics:"
kafka-topics --bootstrap-server "$BOOTSTRAP_SERVER" --list
