package com.mongodb.course.m13.insurance.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document("m13_claim_events")
@TypeAlias("ClaimFiled")
public record ClaimFiled(
        @Id String eventId,
        String aggregateId,
        long version,
        Instant occurredAt,
        String policyId,
        String claimantName,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal claimedAmount,
        String category
) implements ClaimEvent {
}
