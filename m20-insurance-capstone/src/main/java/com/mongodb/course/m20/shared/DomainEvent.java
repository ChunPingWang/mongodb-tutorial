package com.mongodb.course.m20.shared;

import java.time.Instant;

public interface DomainEvent {
    String eventId();
    String aggregateId();
    long version();
    Instant occurredAt();
}
