package com.mongodb.course.m19.infrastructure.saga;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
public class SagaLogRepository {

    private final MongoTemplate mongoTemplate;

    public SagaLogRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public SagaLog save(SagaLog sagaLog) {
        return mongoTemplate.save(sagaLog);
    }

    public Optional<SagaLog> findById(String sagaId) {
        return Optional.ofNullable(mongoTemplate.findById(sagaId, SagaLog.class));
    }

    public void updateStatus(String sagaId, SagaStatus status) {
        var query = Query.query(Criteria.where("_id").is(sagaId));
        var update = new Update().set("status", status);
        if (status == SagaStatus.COMPLETED || status == SagaStatus.COMPENSATED || status == SagaStatus.FAILED) {
            update.set("completedAt", Instant.now());
        }
        mongoTemplate.updateFirst(query, update, SagaLog.class);
    }

    public void updateStep(String sagaId, int stepIndex, StepLog stepLog) {
        var query = Query.query(Criteria.where("_id").is(sagaId));
        var update = new Update()
                .set("steps." + stepIndex, stepLog)
                .set("currentStepIndex", stepIndex);
        mongoTemplate.updateFirst(query, update, SagaLog.class);
    }

    public void updateContext(String sagaId, Map<String, Object> context) {
        var query = Query.query(Criteria.where("_id").is(sagaId));
        var update = new Update().set("context", context);
        mongoTemplate.updateFirst(query, update, SagaLog.class);
    }

    public void updateFailureReason(String sagaId, String reason) {
        var query = Query.query(Criteria.where("_id").is(sagaId));
        var update = new Update().set("failureReason", reason);
        mongoTemplate.updateFirst(query, update, SagaLog.class);
    }
}
