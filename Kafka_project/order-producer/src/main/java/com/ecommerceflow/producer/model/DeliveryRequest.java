package com.ecommerceflow.producer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryRequest {

    private String deliveredTo;
    private boolean signatureRequired;
    private String deliveryNotes;
}
