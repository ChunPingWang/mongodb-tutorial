package com.mongodb.course.m19.infrastructure.saga;

public enum SagaStatus {
    STARTED,
    RUNNING,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}
