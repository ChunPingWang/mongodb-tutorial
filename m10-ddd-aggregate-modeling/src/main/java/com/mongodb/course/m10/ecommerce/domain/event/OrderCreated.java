package com.mongodb.course.m10.ecommerce.domain.event;

import com.mongodb.course.m10.banking.domain.model.Money;
import com.mongodb.course.m10.shared.DomainEvent;

import java.time.Instant;

public record OrderCreated(
        String aggregateId,
        String orderNumber,
        Money totalAmount,
        Instant occurredAt
) implements DomainEvent {
}
