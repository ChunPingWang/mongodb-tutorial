package com.mongodb.course.m12.insurance.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("m12_claim_events")
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
