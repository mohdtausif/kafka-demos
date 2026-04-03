package com.ecommerceflow.analytics.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for aggregating order analytics metrics in-memory.
 */
@Slf4j
@Service
public class AnalyticsService {

    // Order counts by status
    private final Map<String, AtomicLong> orderCountsByStatus = new ConcurrentHashMap<>();

    // Order counts by date
    private final Map<String, AtomicLong> orderCountsByDate = new ConcurrentHashMap<>();

    // Revenue by date
    private final Map<String, BigDecimal> revenueByDate = new ConcurrentHashMap<>();

    // Total counters
    private final AtomicLong totalOrdersCreated = new AtomicLong(0);
    private final AtomicLong totalOrdersPaid = new AtomicLong(0);
    private final AtomicLong totalOrdersShipped = new AtomicLong(0);
    private final AtomicLong totalOrdersDelivered = new AtomicLong(0);

    // Revenue tracking
    private volatile BigDecimal totalRevenue = BigDecimal.ZERO;

    // Top products
    private final Map<String, AtomicLong> productOrderCounts = new ConcurrentHashMap<>();

    public void recordOrderCreated(String orderId, String productId) {
        totalOrdersCreated.incrementAndGet();
        orderCountsByStatus.computeIfAbsent("CREATED", k -> new AtomicLong(0)).incrementAndGet();
        orderCountsByDate.computeIfAbsent(LocalDate.now().toString(), k -> new AtomicLong(0)).incrementAndGet();
        productOrderCounts.computeIfAbsent(productId, k -> new AtomicLong(0)).incrementAndGet();
        log.debug("Analytics: Order CREATED recorded. orderId={}, totalCreated={}", orderId, totalOrdersCreated.get());
    }

    public void recordOrderPaid(String orderId, BigDecimal amount) {
        totalOrdersPaid.incrementAndGet();
        orderCountsByStatus.computeIfAbsent("PAID", k -> new AtomicLong(0)).incrementAndGet();
        synchronized (this) {
            totalRevenue = totalRevenue.add(amount);
            revenueByDate.merge(LocalDate.now().toString(), amount, BigDecimal::add);
        }
        log.debug("Analytics: Order PAID recorded. orderId={}, amount={}, totalRevenue={}", orderId, amount, totalRevenue);
    }

    public void recordOrderShipped(String orderId) {
        totalOrdersShipped.incrementAndGet();
        orderCountsByStatus.computeIfAbsent("SHIPPED", k -> new AtomicLong(0)).incrementAndGet();
        log.debug("Analytics: Order SHIPPED recorded. orderId={}, totalShipped={}", orderId, totalOrdersShipped.get());
    }

    public void recordOrderDelivered(String orderId) {
        totalOrdersDelivered.incrementAndGet();
        orderCountsByStatus.computeIfAbsent("DELIVERED", k -> new AtomicLong(0)).incrementAndGet();
        log.debug("Analytics: Order DELIVERED recorded. orderId={}, totalDelivered={}", orderId, totalOrdersDelivered.get());
    }

    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new ConcurrentHashMap<>();
        metrics.put("totalOrdersCreated", totalOrdersCreated.get());
        metrics.put("totalOrdersPaid", totalOrdersPaid.get());
        metrics.put("totalOrdersShipped", totalOrdersShipped.get());
        metrics.put("totalOrdersDelivered", totalOrdersDelivered.get());
        metrics.put("totalRevenue", totalRevenue);
        metrics.put("orderCountsByStatus", orderCountsByStatus);
        metrics.put("orderCountsByDate", orderCountsByDate);
        metrics.put("revenueByDate", revenueByDate);
        metrics.put("topProducts", productOrderCounts);
        return metrics;
    }
}
