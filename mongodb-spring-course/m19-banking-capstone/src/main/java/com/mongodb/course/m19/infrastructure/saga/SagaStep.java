package com.mongodb.course.m19.infrastructure.saga;

public interface SagaStep {
    String name();
    void execute(SagaContext context);
    void compensate(SagaContext context);
}
