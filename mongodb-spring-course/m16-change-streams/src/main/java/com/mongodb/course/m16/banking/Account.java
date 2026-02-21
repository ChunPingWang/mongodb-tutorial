package com.mongodb.course.m16.banking;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("m16_accounts")
public record Account(
        @Id String id,
        String accountHolder,
        long balance,
        Instant updatedAt
) {
    public static Account of(String accountHolder, long balance) {
        return new Account(null, accountHolder, balance, Instant.now());
    }
}
