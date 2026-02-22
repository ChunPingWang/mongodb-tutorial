package com.mongodb.course.m20.claim.event;

import org.springframework.data.annotation.TypeAlias;

import java.time.Instant;

@TypeAlias("ClaimRejected")
public record ClaimRejected(
        String eventId,
        String aggregateId,
        long version,
        Instant occurredAt,
        String rejectionReason
) implements ClaimEvent {
}
