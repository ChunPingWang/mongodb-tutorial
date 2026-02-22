package com.mongodb.course.m10.shared;

import java.time.Instant;

/**
 * Shared domain event marker interface.
 * In a multi-module project, this would reside in a shared kernel module.
 */
public interface DomainEvent {
    Instant occurredAt();
    String aggregateId();
}
