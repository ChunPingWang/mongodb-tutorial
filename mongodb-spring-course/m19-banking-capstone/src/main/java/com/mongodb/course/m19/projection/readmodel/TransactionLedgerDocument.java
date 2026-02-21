package com.mongodb.course.m19.projection.readmodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document("m19_transaction_ledger")
public record TransactionLedgerDocument(
        @Id String eventId,
        String accountId,
        String transactionType,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal amount,
        String description,
        String counterpartyAccountId,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal balanceAfter,
        Instant occurredAt
) {
}
