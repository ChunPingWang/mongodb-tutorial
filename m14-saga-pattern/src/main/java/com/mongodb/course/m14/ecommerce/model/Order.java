package com.mongodb.course.m14.ecommerce.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("m14_orders")
public record Order(
        @Id String orderId,
        String customerId,
        List<OrderItem> items,
        long totalAmount,
        OrderStatus status
) {
}
