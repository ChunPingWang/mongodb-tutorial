package com.mongodb.course.m10.ecommerce.domain.model;

import java.time.Instant;

public record PaymentInfo(String paymentMethod, String transactionId, Instant paidAt) {

    public PaymentInfo {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("Payment method cannot be blank");
        }
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("Transaction ID cannot be blank");
        }
        if (paidAt == null) {
            throw new IllegalArgumentException("Paid time cannot be null");
        }
    }
}
