package com.mongodb.course.m10.banking.domain.event;

import com.mongodb.course.m10.shared.DomainEvent;

import java.time.Instant;

public record LoanPreliminaryReviewPassed(
        String aggregateId,
        Instant occurredAt
) implements DomainEvent {
}
