package com.mongodb.course.m14.ecommerce.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("m14_payments")
public record Payment(
        @Id String paymentId,
        String sagaId,
        String orderId,
        long amount,
        PaymentStatus status,
        String type
) {
}
