package com.mongodb.course.m16.banking;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("m16_account_notifications")
public record AccountNotification(
        @Id String id,
        String operationType,
        String documentKey,
        String accountHolder,
        Long balance,
        Instant capturedAt
) {
    public static AccountNotification of(String operationType, String documentKey,
                                         String accountHolder, Long balance) {
        return new AccountNotification(null, operationType, documentKey, accountHolder, balance, Instant.now());
    }
}
