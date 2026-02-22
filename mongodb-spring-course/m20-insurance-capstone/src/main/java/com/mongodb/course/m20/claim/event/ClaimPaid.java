package com.mongodb.course.m20.claim.event;

import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@TypeAlias("ClaimPaid")
public record ClaimPaid(
        String eventId,
        String aggregateId,
        long version,
        Instant occurredAt,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal paidAmount,
        String paymentReference
) implements ClaimEvent {
}
