package com.mongodb.course.m21.order.model;

public enum OrderStatus {
    PLACED,
    INVENTORY_RESERVED,
    PAYMENT_PROCESSED,
    CONFIRMED,
    SHIPPED,
    CANCELLED
}
