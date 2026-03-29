# Kafka Security Configuration Guide

## Overview
This document outlines security best practices and configurations for the Kafka Capstone Project in production environments.

## 1. SSL/TLS Configuration

### Generate Self-Signed Certificates (Development Only)
```bash
# Create keystore
keytool -genkey -alias kafka-broker -keyalg RSA -keystore kafka.server.keystore.jks \
  -storepass kafka-pass -keypass kafka-pass

# Create truststore
keytool -import -alias ca-cert -file ca-cert -infile ca-cert.pem \
  -keystore kafka.server.truststore.jks -storepass kafka-pass

# Export certificate  
keytool -export -alias kafka-broker -keystore kafka.server.keystore.jks \
  -file kafka-broker.pem -storepass kafka-pass
```

### Docker Compose SSL Configuration
Add to each kafka broker in docker-compose.yml:
```yaml
environment:
  KAFKA_LISTENERS: SSL://0.0.0.0:9093,PLAINTEXT://0.0.0.0:9092
  KAFKA_ADVERTISED_LISTENERS: SSL://kafka1:9093,PLAINTEXT://localhost:9092
  KAFKA_SSL_KEYSTORE_LOCATION: /var/secrets/kafka.server.keystore.jks
  KAFKA_SSL_KEYSTORE_PASSWORD: kafka-pass
  KAFKA_SSL_KEY_PASSWORD: kafka-pass
  KAFKA_SSL_TRUSTSTORE_LOCATION: /var/secrets/kafka.server.truststore.jks
  KAFKA_SSL_TRUSTSTORE_PASSWORD: kafka-pass
  KAFKA_SSL_CLIENT_AUTH: required
```

## 2. SASL Authentication

### SASL/PLAIN Configuration (Simpler, for POC)

**kafka_server_jaas.conf:**
```
KafkaServer {
  org.apache.kafka.common.security.plain.PlainLoginModule required
  username="admin"
  password="admin-secret"
  user_admin="admin-secret"
  user_producer="producer-secret"
  user_consumer="consumer-secret";
};

Client {
  org.apache.kafka.common.security.plain.PlainLoginModule required
  username="admin"
  password="admin-secret";
};
```

**Docker Compose Addition:**
```yaml
environment:
  KAFKA_SASL_ENABLED_MECHANISMS: PLAIN
  KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL: PLAIN
  KAFKA_SECURITY_INTER_BROKER_PROTOCOL: SASL_SSL
  KAFKA_SASL_JAAS_CONFIG: |
    org.apache.kafka.common.security.plain.PlainLoginModule required
    username="admin" password="admin-secret" 
    user_producer="producer-secret" 
    user_consumer="consumer-secret";
```

### Producer with SASL/PLAIN
```python
producer_config = {
    'bootstrap.servers': 'localhost:9092',
    'security.protocol': 'SASL_PLAIN',
    'sasl.mechanism': 'PLAIN',
    'sasl.username': 'producer',
    'sasl.password': 'producer-secret',
    # ... other configs
}
```

### Consumer with SASL/PLAIN
```python
consumer_config = {
    'bootstrap.servers': 'localhost:9092',
    'security.protocol': 'SASL_PLAIN', 
    'sasl.mechanism': 'PLAIN',
    'sasl.username': 'consumer',
    'sasl.password': 'consumer-secret',
    'group.id': 'analytics-consumer-group',
    # ... other configs
}
```

## 3. ACL (Access Control List) Configuration

### User Management
```bash
# Create users (if using SCRAM authentication)
docker exec kafka-demos-kafka1-1 kafka-configs --bootstrap-server localhost:9092 \
  --alter --add-config 'SCRAM-SHA-256=[password=producer-secret]' \
  --entity-type users --entity-name producer

docker exec kafka-demos-kafka1-1 kafka-configs --bootstrap-server localhost:9092 \
  --alter --add-config 'SCRAM-SHA-256=[password=consumer-secret]' \
  --entity-type users --entity-name consumer
```

### Grant Permissions to Topics

**Producer Permissions:**
```bash
# Producer: Write to order-events topic
docker exec kafka-demos-kafka1-1 kafka-acls --bootstrap-server localhost:9092 \
  --create --allow-principal User:producer \
  --operation Write --topic order-events

# Producer: Create topics
docker exec kafka-demos-kafka1-1 kafka-acls --bootstrap-server localhost:9092 \
  --create --allow-principal User:producer \
  --operation Create --topic-filter ".*"
```

**Analytics Consumer Permissions:**
```bash
# Read from order-events
docker exec kafka-demos-kafka1-1 kafka-acls --bootstrap-server localhost:9092 \
  --create --allow-principal User:consumer \
  --operation Read --topic order-events

# Manage consumer group
docker exec kafka-demos-kafka1-1 kafka-acls --bootstrap-server localhost:9092 \
  --create --allow-principal User:consumer \
  --operation Describe,AlterConfigs --topic order-events

# Consumer group operations
docker exec kafka-demos-kafka1-1 kafka-acls --bootstrap-server localhost:9092 \
  --create --allow-principal User:consumer \
  --operation Read --group analytics-consumer-group
```

## 4. Broker Quotas

Prevent resource exhaustion with client quotas:

```bash
# Limit produce rate for producer user (100MB/sec)
docker exec kafka-demos-kafka1-1 kafka-configs --bootstrap-server localhost:9092 \
  --alter --add-config 'producer_byte_rate=104857600' \
  --entity-type users --entity-name producer

# Limit consume rate for consumer user (50MB/sec)
docker exec kafka-demos-kafka1-1 kafka-configs --bootstrap-server localhost:9092 \
  --alter --add-config 'consumer_byte_rate=52428800' \
  --entity-type users --entity-name consumer

# Limit consumer group fetch rate
docker exec kafka-demos-kafka1-1 kafka-configs --bootstrap-server localhost:9092 \
  --alter --add-config 'consumer_byte_rate=52428800' \
  --entity-type client-ids --entity-name analytics-consumer-group
```

## 5. Audit Logging

Enable authorizer to log all ACL operations:

```yaml
environment:
  KAFKA_AUTHORIZER_CLASS_NAME: kafka.security.authorizer.AclAuthorizer
  KAFKA_SUPER_USERS: "User:admin"
  KAFKA_LOG_RETENTION_HOURS: 168
```

## 6. Monitoring Security Events

### Check ACL Configurations
```bash
# List all ACLs
docker exec kafka-demos-kafka1-1 kafka-acls --bootstrap-server localhost:9092 --list

# List ACLs for specific principal
docker exec kafka-demos-kafka1-1 kafka-acls --bootstrap-server localhost:9092 \
  --list --principal User:producer
```

### Monitor Failed Authentication
```bash
# Check broker logs for authentication failures
docker logs kafka-demos-kafka1-1 | grep -i "authentication failed"

# Monitor with Prometheus
curl http://localhost:9090/api/v1/query?query=kafka_server_authorization_failures
```

## 7. Production Security Checklist

- [ ] SSL/TLS enabled for all broker-to-broker communication
- [ ] SSL/TLS enabled for client-to-broker communication
- [ ] SASL authentication configured and tested
- [ ] ACLs defined for each user/application
- [ ] Super user configured and restricted
- [ ] Client quotas configured to prevent abuse  
- [ ] Audit logging enabled
- [ ] Security monitoring and alerting in place
- [ ] Secrets management (passwords in vault, not in configs)
- [ ] Regular security audits scheduled
- [ ] Incident response procedures documented
- [ ] Regular updates and patches applied

## 8. Vault Integration for Secrets (Optional)

For production, use a secrets management system:

```python
# Example: Using HashiCorp Vault for credentials
import hvac

client = hvac.Client(url='http://localhost:8200')
secret = client.secrets.kv.read_secret_version(path='kafka/prod')

producer_config = {
    'bootstrap.servers': 'kafka:9092',
    'security.protocol': 'SASL_SSL',
    'sasl.mechanism': 'SCRAM-SHA-256',
    'sasl.username': secret['data']['data']['username'],
    'sasl.password': secret['data']['data']['password'],
    # ... other configs
}
```

## 9. Network Segmentation

### Docker Network Configuration
```yaml
networks:
  kafka-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.25.0.0/16
```

Restrict access using firewall rules on the host:
```bash
# Allow Kafka brokers only from producer/consumer hosts
sudo ufw allow from 192.168.1.100 to any port 9092
sudo ufw allow from 192.168.1.101 to any port 9092
```

## 10. References

- [Kafka Security Documentation](https://kafka.apache.org/documentation/#security)
- [SSL Configuration](https://kafka.apache.org/documentation/#brokerconfigs_security)
- [SASL Configuration](https://kafka.apache.org/documentation/#sasl_plain)
- [ACL Configuration](https://kafka.apache.org/documentation/#security_authz)
