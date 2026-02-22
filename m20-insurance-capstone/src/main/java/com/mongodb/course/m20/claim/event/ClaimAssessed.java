package com.mongodb.course.m20.claim.event;

import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@TypeAlias("ClaimAssessed")
public record ClaimAssessed(
        String eventId,
        String aggregateId,
        long version,
        Instant occurredAt,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal assessedAmount,
        String assessmentNotes
) implements ClaimEvent {
}
