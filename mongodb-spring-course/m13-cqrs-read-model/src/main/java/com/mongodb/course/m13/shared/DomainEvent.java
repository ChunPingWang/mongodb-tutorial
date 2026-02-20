package com.mongodb.course.m13.shared;

import java.time.Instant;

public interface DomainEvent {
    String eventId();
    String aggregateId();
    long version();
    Instant occurredAt();
}
