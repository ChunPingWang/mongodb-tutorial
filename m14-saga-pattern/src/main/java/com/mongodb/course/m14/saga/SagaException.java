package com.mongodb.course.m14.saga;

public class SagaException extends RuntimeException {

    private final String sagaId;
    private final String failedStep;

    public SagaException(String sagaId, String failedStep, String message, Throwable cause) {
        super(message, cause);
        this.sagaId = sagaId;
        this.failedStep = failedStep;
    }

    public String getSagaId() {
        return sagaId;
    }

    public String getFailedStep() {
        return failedStep;
    }
}
