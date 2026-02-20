package com.mongodb.course.m13.insurance.readmodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Document("m13_claim_dashboards")
public record ClaimDashboardDocument(
        @Id String claimId,
        String policyId,
        String claimantName,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal claimedAmount,
        String category,
        String currentStatus,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal assessedAmount,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal approvedAmount,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal paidAmount,
        String investigatorName,
        String assessorName,
        String approverName,
        Instant filedAt,
        Instant lastUpdatedAt,
        int eventCount,
        long projectedVersion,
        List<TimelineEntry> timeline
) {
    public record TimelineEntry(
            String eventType,
            Instant occurredAt,
            String description
    ) {
    }
}
