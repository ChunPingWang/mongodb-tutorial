package com.mongodb.course.m21.projection.readmodel;

import java.math.BigDecimal;

public record CategoryMetrics(
        String category,
        int totalOrders,
        int cancelledOrders,
        BigDecimal avgOrderValue,
        BigDecimal totalRevenue
) {
}
