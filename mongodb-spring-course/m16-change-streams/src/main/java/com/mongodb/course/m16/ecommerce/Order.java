package com.mongodb.course.m16.ecommerce;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("m16_orders")
public record Order(
        @Id String id,
        String customerId,
        long totalAmount,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static Order of(String customerId, long totalAmount) {
        var now = Instant.now();
        return new Order(null, customerId, totalAmount, OrderStatus.CREATED, now, now);
    }
}
