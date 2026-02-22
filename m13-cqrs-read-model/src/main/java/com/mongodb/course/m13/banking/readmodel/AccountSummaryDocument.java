package com.mongodb.course.m13.banking.readmodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document("m13_account_summaries")
public record AccountSummaryDocument(
        @Id String accountId,
        String accountHolder,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal currentBalance,
        String currency,
        int totalTransactionCount,
        int depositCount,
        int withdrawalCount,
        int transferCount,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal totalDeposited,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal totalWithdrawn,
        Instant lastActivityAt,
        long projectedVersion
) {
}
