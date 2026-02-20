package com.mongodb.course.m10.banking.domain.event;

import com.mongodb.course.m10.banking.domain.model.Money;
import com.mongodb.course.m10.shared.DomainEvent;

import java.time.Instant;

public record LoanApplicationSubmitted(
        String aggregateId,
        String applicantName,
        Money requestedAmount,
        Instant occurredAt
) implements DomainEvent {
}
