package com.mongodb.course.m14.saga;

public interface SagaStep {
    String name();
    void execute(SagaContext context);
    void compensate(SagaContext context);
}
