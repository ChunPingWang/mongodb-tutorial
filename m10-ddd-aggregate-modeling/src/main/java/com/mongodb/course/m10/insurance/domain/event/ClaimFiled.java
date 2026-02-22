package com.mongodb.course.m10.insurance.domain.event;

import com.mongodb.course.m10.banking.domain.model.Money;
import com.mongodb.course.m10.shared.DomainEvent;

import java.time.Instant;

public record ClaimFiled(
        String aggregateId,
        String policyId,
        Money totalClaimedAmount,
        Instant occurredAt
) implements DomainEvent {
}
