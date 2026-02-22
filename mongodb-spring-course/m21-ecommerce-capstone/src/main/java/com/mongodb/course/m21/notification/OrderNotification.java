package com.mongodb.course.m21.notification;

import java.time.Instant;

public record OrderNotification(
        String notificationId,
        String orderId,
        String newStatus,
        String message,
        Instant occurredAt
) {
}
