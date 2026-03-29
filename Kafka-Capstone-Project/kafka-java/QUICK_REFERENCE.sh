#!/bin/bash
# Quick Reference Commands for Kafka Capstone Java Project

echo "====== Kafka Capstone Project - Java Spring Boot ======"
echo ""

# Build
echo "## Build Commands"
echo "Build project:"
echo "  mvn clean package"
echo ""
echo "Build without tests:"
echo "  mvn clean package -DskipTests"
echo ""

# Run
echo "## Run Commands"
echo "Run Spring Boot application:"
echo "  mvn spring-boot:run"
echo ""
echo "Run JAR file:"
echo "  java -jar target/kafka-capstone-1.0.0.jar"
echo ""

# REST API
echo "## REST API Commands"
echo "Send single event:"
echo "  curl -X POST 'http://localhost:8080/api/producer/send?eventType=ORDER_CREATED'"
echo ""
echo "Send batch of 100 events:"
echo "  curl -X POST 'http://localhost:8080/api/producer/send-batch?count=100'"
echo ""
echo "Send order lifecycle (4 events):"
echo "  curl -X POST 'http://localhost:8080/api/producer/send-lifecycle'"
echo ""
echo "Get analytics metrics:"
echo "  curl http://localhost:8080/api/metrics/analytics | jq ."
echo ""
echo "Get inventory status:"
echo "  curl http://localhost:8080/api/metrics/inventory | jq ."
echo ""
echo "Get notification stats:"
echo "  curl http://localhost:8080/api/metrics/notifications | jq ."
echo ""

# Kafka CLI
echo "## Kafka CLI Commands (from parent docker-compose)"
echo "List topics:"
echo "  docker exec kafka-demos-kafka1-1 kafka-topics --list --bootstrap-server localhost:9092"
echo ""
echo "Describe topic:"
echo "  docker exec kafka-demos-kafka1-1 kafka-topics --describe --topic order-events --bootstrap-server localhost:9092"
echo ""
echo "Consumer group status:"
echo "  docker exec kafka-demos-kafka1-1 kafka-consumer-groups --bootstrap-server localhost:9092 --group analytics-consumer-group --describe"
echo ""

# Testing
echo "## Testing"
echo "Run tests:"
echo "  mvn test"
echo ""
echo "Run specific test:"
echo "  mvn test -Dtest=OrderProducerServiceTest"
echo ""

# Logs
echo "## View Logs"
echo "Tail logs (grep for specific consumer):"
echo "  mvn spring-boot:run | grep 'analytics\|notification\|inventory'"
echo ""

echo "====== End of Quick Reference ======"
