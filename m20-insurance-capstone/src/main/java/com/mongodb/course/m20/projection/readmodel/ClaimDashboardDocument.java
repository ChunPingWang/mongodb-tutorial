package com.mongodb.course.m20.projection.readmodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ClaimDashboardDocument(
        @Id String claimId,
        String policyId,
        String policyType,
        String claimantName,
        String category,
        String status,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal claimedAmount,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal assessedAmount,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal approvedAmount,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal paidAmount,
        String fraudRisk,
        List<TimelineEntry> timeline,
        Instant lastUpdatedAt,
        long projectedVersion
) {
}
