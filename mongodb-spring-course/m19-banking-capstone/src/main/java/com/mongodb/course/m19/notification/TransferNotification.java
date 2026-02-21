package com.mongodb.course.m19.notification;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferNotification(
        String eventId,
        String accountId,
        String eventType,
        BigDecimal amount,
        Instant occurredAt
) {
}
