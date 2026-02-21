package com.mongodb.course.m19.projection.readmodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document("m19_account_summaries")
public record AccountSummaryDocument(
        @Id String accountId,
        String accountHolder,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal currentBalance,
        String currency,
        int totalTransactions,
        int depositCount,
        int withdrawalCount,
        int transferOutCount,
        int transferInCount,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal totalInterestEarned,
        boolean closed,
        Instant lastActivityAt,
        long projectedVersion
) {
}
