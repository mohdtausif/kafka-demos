package com.ecommerceflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Boot application for Kafka Capstone Project
 * Event-driven pipeline for e-commerce order processing
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
public class KafkaCapstoneApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaCapstoneApplication.class, args);
        log.info("====================================================");
        log.info("Kafka Capstone Project Application Started");
        log.info("====================================================");
        log.info("Available endpoints:");
        log.info("  POST /api/producer/send - Send single order event");
        log.info("  POST /api/producer/send-batch - Send batch of order events");
        log.info("  GET  /api/metrics/analytics - Get analytics metrics");
        log.info("  GET  /api/metrics/inventory - Get inventory status");
        log.info("  GET  /api/metrics/notifications - Get notification stats");
        log.info("====================================================");
    }
}
