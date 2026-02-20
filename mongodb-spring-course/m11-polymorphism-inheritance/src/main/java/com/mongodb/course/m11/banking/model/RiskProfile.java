package com.mongodb.course.m11.banking.model;

public record RiskProfile(int level, String category) {

    public RiskProfile {
        if (level < 1 || level > 5) {
            throw new IllegalArgumentException("Risk level must be between 1 and 5");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category cannot be blank");
        }
    }
}
