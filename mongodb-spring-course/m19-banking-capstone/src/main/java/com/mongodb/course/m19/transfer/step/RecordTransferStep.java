package com.mongodb.course.m19.transfer.step;

import com.mongodb.course.m19.infrastructure.saga.SagaContext;
import com.mongodb.course.m19.infrastructure.saga.SagaStep;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class RecordTransferStep implements SagaStep {

    private static final String COLLECTION = "m19_transaction_ledger";

    private final MongoTemplate mongoTemplate;

    public RecordTransferStep(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public String name() {
        return "RECORD_TRANSFER";
    }

    @Override
    public void execute(SagaContext context) {
        // Transfer ledger entries already inserted by debit/credit steps via projectors.
        // This step is a confirmation point — nothing additional needed.
    }

    @Override
    public void compensate(SagaContext context) {
        // Compensation for debit/credit already handles reversal entries.
    }
}
