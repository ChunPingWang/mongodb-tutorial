package com.mongodb.course.m10.ecommerce.domain.event;

import com.mongodb.course.m10.shared.DomainEvent;

import java.time.Instant;

public record OrderPaid(
        String aggregateId,
        String transactionId,
        Instant occurredAt
) implements DomainEvent {
}
