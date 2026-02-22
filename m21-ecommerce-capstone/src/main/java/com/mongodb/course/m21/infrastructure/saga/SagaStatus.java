package com.mongodb.course.m21.infrastructure.saga;

public enum SagaStatus {
    STARTED,
    RUNNING,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}
