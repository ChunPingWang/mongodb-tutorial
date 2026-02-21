package com.mongodb.course.m14.insurance.service;

import com.mongodb.course.m14.insurance.model.Claim;
import com.mongodb.course.m14.insurance.step.ApproveClaimStep;
import com.mongodb.course.m14.insurance.step.CreateSettlementPaymentStep;
import com.mongodb.course.m14.insurance.step.NotifyCustomerStep;
import com.mongodb.course.m14.insurance.step.UpdatePolicyStep;
import com.mongodb.course.m14.saga.SagaContext;
import com.mongodb.course.m14.saga.SagaOrchestrator;
import com.mongodb.course.m14.saga.SagaStep;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClaimSettlementSagaService {

    private final SagaOrchestrator orchestrator;
    private final MongoTemplate mongoTemplate;

    public ClaimSettlementSagaService(SagaOrchestrator orchestrator, MongoTemplate mongoTemplate) {
        this.orchestrator = orchestrator;
        this.mongoTemplate = mongoTemplate;
    }

    public String settleClaim(String claimId) {
        var claim = mongoTemplate.findById(claimId, Claim.class);
        if (claim == null) {
            throw new IllegalArgumentException("Claim not found: " + claimId);
        }

        List<SagaStep> steps = List.of(
                new ApproveClaimStep(mongoTemplate),
                new CreateSettlementPaymentStep(mongoTemplate),
                new UpdatePolicyStep(mongoTemplate),
                new NotifyCustomerStep(mongoTemplate)
        );

        var context = new SagaContext();
        context.put("claimId", claimId);
        context.put("policyId", claim.policyId());
        context.put("claimantName", claim.claimantName());
        context.put("amount", claim.amount());

        return orchestrator.execute("CLAIM_SETTLEMENT_SAGA", steps, context);
    }
}
