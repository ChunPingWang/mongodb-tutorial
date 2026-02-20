package com.mongodb.course.m10.insurance.domain.event;

import com.mongodb.course.m10.shared.DomainEvent;

import java.time.Instant;

public record ClaimRejected(
        String aggregateId,
        String reason,
        Instant occurredAt
) implements DomainEvent {
}
