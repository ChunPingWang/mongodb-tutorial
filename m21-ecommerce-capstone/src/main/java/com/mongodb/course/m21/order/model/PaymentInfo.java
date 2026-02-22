package com.mongodb.course.m21.order.model;

public record PaymentInfo(
        String transactionId,
        String method,
        String last4Digits
) {
}
