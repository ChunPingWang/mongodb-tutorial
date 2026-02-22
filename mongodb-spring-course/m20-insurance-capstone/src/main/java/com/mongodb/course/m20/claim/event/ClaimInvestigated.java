package com.mongodb.course.m20.claim.event;

import org.springframework.data.annotation.TypeAlias;

import java.time.Instant;

@TypeAlias("ClaimInvestigated")
public record ClaimInvestigated(
        String eventId,
        String aggregateId,
        long version,
        Instant occurredAt,
        String investigatorName,
        String findings,
        String fraudRisk
) implements ClaimEvent {
}
