package com.mongodb.course.m21.projection.readmodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document("m21_sales_statistics")
public record SalesStatisticsDocument(
        @Id String category,
        Integer totalOrders,
        Integer confirmedCount,
        Integer cancelledCount,
        Integer totalItemsSold,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal totalRevenue,
        Instant lastUpdatedAt
) {
}
