package com.mongodb.course.m20.infrastructure.saga;

public interface SagaStep {
    String name();
    void execute(SagaContext context);
    void compensate(SagaContext context);
}
