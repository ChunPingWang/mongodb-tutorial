package com.mongodb.course.m20.claim.event;

import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@TypeAlias("ClaimFiled")
public record ClaimFiled(
        String eventId,
        String aggregateId,
        long version,
        Instant occurredAt,
        String policyId,
        String claimantName,
        String category,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal claimedAmount,
        String description
) implements ClaimEvent {
}
