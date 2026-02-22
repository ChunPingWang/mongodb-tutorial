package com.mongodb.course.m20.projection.readmodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

public record ClaimStatisticsDocument(
        @Id String category,
        Integer totalClaims,
        Integer filedCount,
        Integer investigatedCount,
        Integer assessedCount,
        Integer approvedCount,
        Integer rejectedCount,
        Integer paidCount,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal totalClaimedAmount,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal totalApprovedAmount,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal totalPaidAmount
) {
    public int totalClaimsOrZero() { return totalClaims != null ? totalClaims : 0; }
    public int filedCountOrZero() { return filedCount != null ? filedCount : 0; }
    public int paidCountOrZero() { return paidCount != null ? paidCount : 0; }
    public int approvedCountOrZero() { return approvedCount != null ? approvedCount : 0; }
}
