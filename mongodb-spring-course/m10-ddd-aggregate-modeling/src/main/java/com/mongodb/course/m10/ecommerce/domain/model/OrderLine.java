package com.mongodb.course.m10.ecommerce.domain.model;

import com.mongodb.course.m10.banking.domain.model.Money;

public record OrderLine(String productId, String productName, int quantity, Money unitPrice) {

    public OrderLine {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Product ID cannot be blank");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("Unit price cannot be null");
        }
    }

    public Money lineTotal() {
        return unitPrice.multiply(quantity);
    }
}
