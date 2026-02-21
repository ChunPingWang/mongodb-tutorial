package com.mongodb.course.m14.ecommerce.model;

public record OrderItem(
        String productId,
        String productName,
        int quantity,
        long unitPrice
) {
    public long subtotal() {
        return quantity * unitPrice;
    }
}
