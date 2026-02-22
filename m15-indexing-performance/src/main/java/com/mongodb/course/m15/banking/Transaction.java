package com.mongodb.course.m15.banking;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("m15_transactions")
public record Transaction(
        @Id String id,
        String accountId,
        Instant transactionDate,
        TransactionType type,
        long amount,
        String description,
        String category,
        Instant createdAt
) {
    public static Transaction of(String accountId, Instant transactionDate,
                                  TransactionType type, long amount,
                                  String description, String category) {
        return new Transaction(null, accountId, transactionDate, type, amount,
                description, category, Instant.now());
    }

    public static Transaction ofWithCreatedAt(String accountId, Instant transactionDate,
                                               TransactionType type, long amount,
                                               String description, String category,
                                               Instant createdAt) {
        return new Transaction(null, accountId, transactionDate, type, amount,
                description, category, createdAt);
    }
}
