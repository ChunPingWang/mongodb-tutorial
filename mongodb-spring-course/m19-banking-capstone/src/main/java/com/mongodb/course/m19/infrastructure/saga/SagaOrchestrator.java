package com.mongodb.course.m19.infrastructure.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);

    private final SagaLogRepository sagaLogRepository;

    public SagaOrchestrator(SagaLogRepository sagaLogRepository) {
        this.sagaLogRepository = sagaLogRepository;
    }

    public String execute(String sagaType, List<SagaStep> steps, SagaContext context) {
        String sagaId = UUID.randomUUID().toString();
        context.put("sagaId", sagaId);
        List<String> stepNames = steps.stream().map(SagaStep::name).toList();

        var sagaLog = SagaLog.create(sagaId, sagaType, stepNames, context.toMap());
        sagaLogRepository.save(sagaLog);
        sagaLogRepository.updateStatus(sagaId, SagaStatus.RUNNING);

        int lastCompletedIndex = -1;

        for (int i = 0; i < steps.size(); i++) {
            var step = steps.get(i);
            try {
                log.info("Saga [{}] executing step {}: {}", sagaId, i, step.name());
                step.execute(context);
                lastCompletedIndex = i;
                sagaLogRepository.updateStep(sagaId, i, StepLog.pending(step.name()).succeeded());
                sagaLogRepository.updateContext(sagaId, context.toMap());
            } catch (Exception e) {
                log.warn("Saga [{}] step {} failed: {}", sagaId, step.name(), e.getMessage());
                sagaLogRepository.updateStep(sagaId, i, StepLog.pending(step.name()).failed(e.getMessage()));
                sagaLogRepository.updateFailureReason(sagaId, e.getMessage());
                compensate(sagaId, steps, context, lastCompletedIndex);
                return sagaId;
            }
        }

        sagaLogRepository.updateStatus(sagaId, SagaStatus.COMPLETED);
        log.info("Saga [{}] completed successfully", sagaId);
        return sagaId;
    }

    private void compensate(String sagaId, List<SagaStep> steps, SagaContext context, int lastCompletedIndex) {
        sagaLogRepository.updateStatus(sagaId, SagaStatus.COMPENSATING);
        boolean compensationFailed = false;

        for (int i = lastCompletedIndex; i >= 0; i--) {
            var step = steps.get(i);
            try {
                log.info("Saga [{}] compensating step {}: {}", sagaId, i, step.name());
                step.compensate(context);
                sagaLogRepository.updateStep(sagaId, i, StepLog.pending(step.name()).succeeded().compensated());
            } catch (Exception e) {
                log.error("Saga [{}] compensation failed for step {}: {}", sagaId, step.name(), e.getMessage());
                compensationFailed = true;
            }
        }

        if (compensationFailed) {
            sagaLogRepository.updateStatus(sagaId, SagaStatus.FAILED);
        } else {
            sagaLogRepository.updateStatus(sagaId, SagaStatus.COMPENSATED);
        }
    }
}
