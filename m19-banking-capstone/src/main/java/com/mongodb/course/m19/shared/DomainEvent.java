package com.mongodb.course.m19.shared;

import java.time.Instant;

public interface DomainEvent {
    String eventId();
    String aggregateId();
    long version();
    Instant occurredAt();
}
