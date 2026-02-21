package com.mongodb.course.m17.banking;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("m17_transactions")
public record Transaction(
        @Id String id,
        String accountId,
        long amount,
        String type,
        Instant createdAt
) {
    public static Transaction of(String accountId, long amount, String type) {
        return new Transaction(null, accountId, amount, type, Instant.now());
    }
}
