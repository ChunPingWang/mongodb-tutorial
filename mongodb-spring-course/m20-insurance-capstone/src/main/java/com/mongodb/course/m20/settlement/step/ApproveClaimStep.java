package com.mongodb.course.m20.settlement.step;

import com.mongodb.course.m20.claim.service.ClaimCommandService;
import com.mongodb.course.m20.infrastructure.saga.SagaContext;
import com.mongodb.course.m20.infrastructure.saga.SagaStep;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ApproveClaimStep implements SagaStep {

    private final ClaimCommandService claimCommandService;

    public ApproveClaimStep(ClaimCommandService claimCommandService) {
        this.claimCommandService = claimCommandService;
    }

    @Override
    public String name() {
        return "APPROVE_CLAIM";
    }

    @Override
    public void execute(SagaContext context) {
        String claimId = context.get("claimId", String.class);
        BigDecimal amount = new BigDecimal(context.get("assessedAmount", String.class));

        claimCommandService.approve(claimId, amount);
        context.put("approvedAmount", amount.toPlainString());
    }

    @Override
    public void compensate(SagaContext context) {
        String claimId = context.get("claimId", String.class);
        claimCommandService.reject(claimId, "Settlement saga compensation");
    }
}
