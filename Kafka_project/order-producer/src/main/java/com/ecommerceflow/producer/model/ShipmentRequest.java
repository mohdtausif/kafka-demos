package com.ecommerceflow.producer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentRequest {

    private String trackingNumber;
    private String carrier;
    private String shippingAddress;
    private String estimatedDelivery;
}
