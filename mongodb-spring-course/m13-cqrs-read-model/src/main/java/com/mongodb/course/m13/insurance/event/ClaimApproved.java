package com.mongodb.course.m13.insurance.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document("m13_claim_events")
@TypeAlias("ClaimApproved")
public record ClaimApproved(
        @Id String eventId,
        String aggregateId,
        long version,
        Instant occurredAt,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal approvedAmount,
        String approverName
) implements ClaimEvent {
}
