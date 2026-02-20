package com.mongodb.course.m10.insurance.domain.event;

import com.mongodb.course.m10.banking.domain.model.Money;
import com.mongodb.course.m10.shared.DomainEvent;

import java.time.Instant;

public record ClaimApproved(
        String aggregateId,
        Money approvedAmount,
        Instant occurredAt
) implements DomainEvent {
}
