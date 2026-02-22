package com.mongodb.course.m13.insurance.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("m13_claim_events")
@TypeAlias("ClaimRejected")
public record ClaimRejected(
        @Id String eventId,
        String aggregateId,
        long version,
        Instant occurredAt,
        String reason,
        String rejectorName
) implements ClaimEvent {
}
