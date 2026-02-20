package com.mongodb.course.m10.ecommerce.domain.event;

import com.mongodb.course.m10.shared.DomainEvent;

import java.time.Instant;

public record OrderCompleted(
        String aggregateId,
        Instant occurredAt
) implements DomainEvent {
}
