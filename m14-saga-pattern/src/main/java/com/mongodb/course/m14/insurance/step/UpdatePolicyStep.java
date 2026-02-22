package com.mongodb.course.m14.insurance.step;

import com.mongodb.course.m14.insurance.model.Claim;
import com.mongodb.course.m14.insurance.model.ClaimSettlementStatus;
import com.mongodb.course.m14.insurance.model.Policy;
import com.mongodb.course.m14.saga.SagaContext;
import com.mongodb.course.m14.saga.SagaStep;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public final class UpdatePolicyStep implements SagaStep {

    private final MongoTemplate mongoTemplate;

    public UpdatePolicyStep(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public String name() {
        return "UPDATE_POLICY";
    }

    @Override
    public void execute(SagaContext context) {
        String policyId = context.get("policyId", String.class);
        String claimId = context.get("claimId", String.class);
        long amount = context.get("amount", Long.class);

        var policy = mongoTemplate.findById(policyId, Policy.class);
        if (policy == null) {
            throw new IllegalStateException("Policy not found: " + policyId);
        }

        if (policy.paidClaimsTotal() + amount > policy.coverageAmount()) {
            throw new IllegalStateException(
                    "Claim amount " + amount + " would exceed coverage. " +
                    "Paid: " + policy.paidClaimsTotal() + ", Coverage: " + policy.coverageAmount()
            );
        }

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(policyId)),
                new Update().inc("paidClaimsTotal", amount),
                Policy.class
        );

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(claimId)),
                Update.update("settlementStatus", ClaimSettlementStatus.POLICY_UPDATED),
                Claim.class
        );
    }

    @Override
    public void compensate(SagaContext context) {
        String policyId = context.get("policyId", String.class);
        String claimId = context.get("claimId", String.class);
        long amount = context.get("amount", Long.class);

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(policyId)),
                new Update().inc("paidClaimsTotal", -amount),
                Policy.class
        );

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(claimId)),
                Update.update("settlementStatus", ClaimSettlementStatus.APPROVED),
                Claim.class
        );
    }
}
