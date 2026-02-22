package com.mongodb.course.m21.projection.readmodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Document("m21_order_dashboard")
public record OrderDashboardDocument(
        @Id String orderId,
        String customerId,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal totalAmount,
        String status,
        Integer itemCount,
        List<String> categories,
        String shippingCity,
        String trackingNumber,
        List<TimelineEntry> timeline,
        Instant lastUpdatedAt,
        long projectedVersion
) {
}
