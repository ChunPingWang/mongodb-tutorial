package com.mongodb.course.m19.infrastructure.saga;

import java.time.Instant;

public record StepLog(
        String stepName,
        String status,
        Instant executedAt,
        Instant compensatedAt,
        String errorMessage
) {

    public static StepLog pending(String stepName) {
        return new StepLog(stepName, "PENDING", null, null, null);
    }

    public StepLog succeeded() {
        return new StepLog(stepName, "SUCCEEDED", Instant.now(), null, null);
    }

    public StepLog failed(String error) {
        return new StepLog(stepName, "FAILED", Instant.now(), null, error);
    }

    public StepLog compensated() {
        return new StepLog(stepName, "COMPENSATED", executedAt, Instant.now(), errorMessage);
    }
}
