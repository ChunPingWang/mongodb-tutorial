package com.mongodb.course.m10.ecommerce.domain.model;

public enum OrderStatus {
    CREATED, PAID, SHIPPED, DELIVERED, COMPLETED, RETURNED;

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case CREATED -> target == PAID;
            case PAID -> target == SHIPPED;
            case SHIPPED -> target == DELIVERED;
            case DELIVERED -> target == COMPLETED || target == RETURNED;
            case COMPLETED -> target == RETURNED;
            case RETURNED -> false;
        };
    }
}
