package com.mongodb.course.m20.notification;

import java.time.Instant;

public record ClaimStatusNotification(
        String notificationId,
        String claimId,
        String newStatus,
        String message,
        Instant occurredAt
) {
}
