package com.mongodb.course.m14.ecommerce.model;

public enum OrderStatus {
    PENDING,
    INVENTORY_RESERVED,
    PAYMENT_PROCESSED,
    CONFIRMED,
    CANCELLED
}
