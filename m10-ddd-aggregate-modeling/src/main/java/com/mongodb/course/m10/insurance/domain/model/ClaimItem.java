package com.mongodb.course.m10.insurance.domain.model;

import com.mongodb.course.m10.banking.domain.model.Money;

public record ClaimItem(String description, Money amount, String category) {

    public ClaimItem {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be blank");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category cannot be blank");
        }
    }
}
