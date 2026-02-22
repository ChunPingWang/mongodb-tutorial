package com.mongodb.course.m14.insurance.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("m14_notifications")
public record Notification(
        @Id String notificationId,
        String recipient,
        String claimId,
        String message,
        NotificationStatus status
) {
}
