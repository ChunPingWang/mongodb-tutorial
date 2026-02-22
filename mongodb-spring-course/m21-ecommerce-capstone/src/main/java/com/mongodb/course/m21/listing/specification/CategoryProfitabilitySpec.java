package com.mongodb.course.m21.listing.specification;

import com.mongodb.course.m21.projection.readmodel.CategoryMetrics;

import java.math.BigDecimal;

public class CategoryProfitabilitySpec {

    private static final BigDecimal MIN_AVG_ORDER_VALUE = new BigDecimal("500");
    private static final BigDecimal MAX_CANCEL_RATE = new BigDecimal("0.3");
    private static final int MIN_TOTAL_ORDERS = 5;

    public boolean isSatisfiedBy(CategoryMetrics metrics) {
        if (metrics.totalOrders() < MIN_TOTAL_ORDERS) {
            return false;
        }
        if (metrics.avgOrderValue().compareTo(MIN_AVG_ORDER_VALUE) < 0) {
            return false;
        }
        if (metrics.totalOrders() > 0) {
            BigDecimal cancelRate = BigDecimal.valueOf(metrics.cancelledOrders())
                    .divide(BigDecimal.valueOf(metrics.totalOrders()), 4, java.math.RoundingMode.HALF_UP);
            if (cancelRate.compareTo(MAX_CANCEL_RATE) >= 0) {
                return false;
            }
        }
        return true;
    }

    public String getRejectionReason(CategoryMetrics metrics) {
        if (metrics.totalOrders() < MIN_TOTAL_ORDERS) {
            return "Category has insufficient order history: " + metrics.totalOrders() + " orders";
        }
        if (metrics.avgOrderValue().compareTo(MIN_AVG_ORDER_VALUE) < 0) {
            return "Category average order value too low: " + metrics.avgOrderValue();
        }
        if (metrics.totalOrders() > 0) {
            BigDecimal cancelRate = BigDecimal.valueOf(metrics.cancelledOrders())
                    .divide(BigDecimal.valueOf(metrics.totalOrders()), 4, java.math.RoundingMode.HALF_UP);
            if (cancelRate.compareTo(MAX_CANCEL_RATE) >= 0) {
                return "Category cancel rate too high: " + cancelRate;
            }
        }
        return "Unknown rejection reason";
    }
}
