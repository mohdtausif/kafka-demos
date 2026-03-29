package com.ecommerceflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Analytics metrics for order processing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderMetrics {
    private Long totalOrders;
    private Double totalRevenue;
    private Long eventTimeMillis;
}
