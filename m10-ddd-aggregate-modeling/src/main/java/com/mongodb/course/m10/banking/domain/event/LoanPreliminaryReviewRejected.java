package com.mongodb.course.m10.banking.domain.event;

import com.mongodb.course.m10.shared.DomainEvent;

import java.time.Instant;

public record LoanPreliminaryReviewRejected(
        String aggregateId,
        String reason,
        Instant occurredAt
) implements DomainEvent {
}
