# Schema Evolution Strategy

## Overview

EcommerceFlow uses **Confluent Schema Registry** with **BACKWARD** compatibility mode to ensure safe schema evolution. All event schemas are defined in Apache Avro format.

## Compatibility Mode: BACKWARD

The Schema Registry is configured with `BACKWARD` compatibility, meaning:
- **New consumers can read data written by old producers**
- New schemas can only add fields with default values or remove optional fields
- Existing fields cannot change type

## Schemas

| Schema | Topic | Version | Fields |
|--------|-------|---------|--------|
| `order_created` | order.created | 1 | orderId, customerId, productId, productName, quantity, unitPrice, totalAmount, currency, shippingAddress, status, createdAt, metadata |
| `order_paid` | order.paid | 1 | orderId, customerId, productId, totalAmount, currency, paymentMethod, transactionId, status, paidAt, metadata |
| `order_shipped` | order.shipped | 1 | orderId, customerId, productId, quantity, trackingNumber, carrier, shippingAddress, estimatedDelivery, status, shippedAt, metadata |
| `order_delivered` | order.delivered | 1 | orderId, customerId, productId, quantity, deliveredTo, signatureRequired, deliveryNotes, status, deliveredAt, metadata |

## Evolution Rules

### Safe Changes (BACKWARD compatible)
1. **Add a new field with a default value** — Existing consumers ignore unknown fields
   ```json
   {"name": "loyalty_points", "type": "int", "default": 0}
   ```
2. **Remove an optional field** (one that has a default or is a union with null)
3. **Add a new enum value** to the end of an existing enum

### Unsafe Changes (Will be rejected)
1. Removing a required field (no default value)
2. Changing a field's type (e.g., `string` → `int`)
3. Renaming a field
4. Removing an enum value

## Schema Registration

Schemas are automatically registered when the first message is published by the producer. The Avro serializer registers the schema under the subject `<topic-name>-value`.

### Manual Registration
```bash
# Register a schema manually
curl -X POST http://localhost:8081/subjects/order.created-value/versions \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{"schema": "{...}"}'

# Check compatibility before evolving
curl -X POST http://localhost:8081/compatibility/subjects/order.created-value/versions/latest \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{"schema": "{...}"}'
```

## Evolution Example

Adding a `discount` field to `order_created` (v2):
```json
{
  "name": "discount",
  "type": ["null", {"type": "bytes", "logicalType": "decimal", "precision": 10, "scale": 2}],
  "default": null,
  "doc": "Discount amount applied to the order"
}
```

This is backward compatible because existing consumers simply ignore the new field.
