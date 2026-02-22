package com.mongodb.course.m14.insurance.step;

import com.mongodb.course.m14.insurance.model.Claim;
import com.mongodb.course.m14.insurance.model.ClaimSettlementStatus;
import com.mongodb.course.m14.saga.SagaContext;
import com.mongodb.course.m14.saga.SagaStep;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public final class ApproveClaimStep implements SagaStep {

    private final MongoTemplate mongoTemplate;

    public ApproveClaimStep(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public String name() {
        return "APPROVE_CLAIM";
    }

    @Override
    public void execute(SagaContext context) {
        String claimId = context.get("claimId", String.class);

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(claimId)),
                Update.update("settlementStatus", ClaimSettlementStatus.APPROVED),
                Claim.class
        );
    }

    @Override
    public void compensate(SagaContext context) {
        String claimId = context.get("claimId", String.class);

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(claimId)),
                Update.update("settlementStatus", ClaimSettlementStatus.PENDING),
                Claim.class
        );
    }
}
