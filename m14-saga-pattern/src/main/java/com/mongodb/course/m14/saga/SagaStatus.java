package com.mongodb.course.m14.saga;

public enum SagaStatus {
    STARTED,
    RUNNING,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}
