package com.mongodb.course.m20.infrastructure.saga;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document("m20_settlement_saga_logs")
public record SagaLog(
        @Id String sagaId,
        String sagaType,
        SagaStatus status,
        int currentStepIndex,
        List<StepLog> steps,
        Map<String, Object> context,
        Instant startedAt,
        Instant completedAt,
        String failureReason
) {

    public static SagaLog create(String sagaId, String sagaType, List<String> stepNames, Map<String, Object> initialContext) {
        List<StepLog> steps = stepNames.stream()
                .map(StepLog::pending)
                .toList();
        return new SagaLog(sagaId, sagaType, SagaStatus.STARTED, 0, steps, initialContext, Instant.now(), null, null);
    }
}
