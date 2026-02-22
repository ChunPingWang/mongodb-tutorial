package com.mongodb.course.m16.ecommerce;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("m16_order_audit_log")
public record OrderAuditEntry(
        @Id String id,
        String orderId,
        OrderStatus newStatus,
        Instant changedAt
) {
    public static OrderAuditEntry of(String orderId, OrderStatus newStatus) {
        return new OrderAuditEntry(null, orderId, newStatus, Instant.now());
    }
}
