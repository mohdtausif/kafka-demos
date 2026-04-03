#!/bin/bash
# ============================================
# Generate SSL/TLS Certificates for Kafka
# ============================================

VALIDITY=365
PASSWORD=kafkassl123
CA_CN="EcommerceFlow-CA"
CERT_DIR="$(dirname "$0")/certs"

mkdir -p "$CERT_DIR"

echo "=== Generating CA ==="
openssl req -new -x509 -keyout "$CERT_DIR/ca-key" -out "$CERT_DIR/ca-cert" \
  -days $VALIDITY -passout pass:$PASSWORD \
  -subj "/CN=$CA_CN/O=EcommerceFlow/L=Global"

echo ""
echo "=== Generating Broker Keystores ==="
for i in 1 2 3; do
  BROKER="kafka-$i"
  echo "--- Generating keystore for $BROKER ---"

  # Create broker keystore
  keytool -genkey -noprompt -alias $BROKER -keyalg RSA -keysize 2048 \
    -dname "CN=$BROKER,O=EcommerceFlow,L=Global" \
    -keystore "$CERT_DIR/$BROKER.keystore.jks" \
    -storepass $PASSWORD -keypass $PASSWORD \
    -validity $VALIDITY

  # Create CSR
  keytool -certreq -alias $BROKER \
    -keystore "$CERT_DIR/$BROKER.keystore.jks" \
    -file "$CERT_DIR/$BROKER.csr" \
    -storepass $PASSWORD

  # Sign with CA
  openssl x509 -req -CA "$CERT_DIR/ca-cert" -CAkey "$CERT_DIR/ca-key" \
    -in "$CERT_DIR/$BROKER.csr" -out "$CERT_DIR/$BROKER-signed.crt" \
    -days $VALIDITY -CAcreateserial -passin pass:$PASSWORD

  # Import CA cert into keystore
  keytool -import -noprompt -alias ca-root \
    -keystore "$CERT_DIR/$BROKER.keystore.jks" \
    -file "$CERT_DIR/ca-cert" \
    -storepass $PASSWORD

  # Import signed cert into keystore
  keytool -import -noprompt -alias $BROKER \
    -keystore "$CERT_DIR/$BROKER.keystore.jks" \
    -file "$CERT_DIR/$BROKER-signed.crt" \
    -storepass $PASSWORD

  # Create truststore with CA cert
  keytool -import -noprompt -alias ca-root \
    -keystore "$CERT_DIR/$BROKER.truststore.jks" \
    -file "$CERT_DIR/ca-cert" \
    -storepass $PASSWORD
done

echo ""
echo "=== Generating Client Keystore ==="
keytool -genkey -noprompt -alias client -keyalg RSA -keysize 2048 \
  -dname "CN=client,O=EcommerceFlow,L=Global" \
  -keystore "$CERT_DIR/client.keystore.jks" \
  -storepass $PASSWORD -keypass $PASSWORD \
  -validity $VALIDITY

keytool -certreq -alias client \
  -keystore "$CERT_DIR/client.keystore.jks" \
  -file "$CERT_DIR/client.csr" \
  -storepass $PASSWORD

openssl x509 -req -CA "$CERT_DIR/ca-cert" -CAkey "$CERT_DIR/ca-key" \
  -in "$CERT_DIR/client.csr" -out "$CERT_DIR/client-signed.crt" \
  -days $VALIDITY -CAcreateserial -passin pass:$PASSWORD

keytool -import -noprompt -alias ca-root \
  -keystore "$CERT_DIR/client.keystore.jks" \
  -file "$CERT_DIR/ca-cert" \
  -storepass $PASSWORD

keytool -import -noprompt -alias client \
  -keystore "$CERT_DIR/client.keystore.jks" \
  -file "$CERT_DIR/client-signed.crt" \
  -storepass $PASSWORD

keytool -import -noprompt -alias ca-root \
  -keystore "$CERT_DIR/client.truststore.jks" \
  -file "$CERT_DIR/ca-cert" \
  -storepass $PASSWORD

echo ""
echo "=== SSL Certificate Generation Complete ==="
echo "Certificates stored in: $CERT_DIR"
ls -la "$CERT_DIR"
