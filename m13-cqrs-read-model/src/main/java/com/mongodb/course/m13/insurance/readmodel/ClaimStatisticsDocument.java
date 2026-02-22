package com.mongodb.course.m13.insurance.readmodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document("m13_claim_statistics")
public record ClaimStatisticsDocument(
        @Id String category,
        int totalClaims,
        int filedCount,
        int approvedCount,
        int rejectedCount,
        int paidCount,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal totalClaimedAmount,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal totalApprovedAmount,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal totalPaidAmount,
        Instant lastUpdatedAt
) {
}
