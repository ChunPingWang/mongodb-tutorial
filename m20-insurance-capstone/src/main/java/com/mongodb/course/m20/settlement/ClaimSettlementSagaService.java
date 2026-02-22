package com.mongodb.course.m20.settlement;

import com.mongodb.course.m20.infrastructure.saga.SagaContext;
import com.mongodb.course.m20.infrastructure.saga.SagaOrchestrator;
import com.mongodb.course.m20.infrastructure.saga.SagaStep;
import com.mongodb.course.m20.settlement.step.ApproveClaimStep;
import com.mongodb.course.m20.settlement.step.FraudCheckStep;
import com.mongodb.course.m20.settlement.step.NotifySettlementStep;
import com.mongodb.course.m20.settlement.step.UpdatePolicyStep;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ClaimSettlementSagaService {

    private final SagaOrchestrator orchestrator;
    private final FraudCheckStep fraudCheckStep;
    private final ApproveClaimStep approveClaimStep;
    private final UpdatePolicyStep updatePolicyStep;
    private final NotifySettlementStep notifySettlementStep;

    public ClaimSettlementSagaService(SagaOrchestrator orchestrator,
                                      FraudCheckStep fraudCheckStep,
                                      ApproveClaimStep approveClaimStep,
                                      UpdatePolicyStep updatePolicyStep,
                                      NotifySettlementStep notifySettlementStep) {
        this.orchestrator = orchestrator;
        this.fraudCheckStep = fraudCheckStep;
        this.approveClaimStep = approveClaimStep;
        this.updatePolicyStep = updatePolicyStep;
        this.notifySettlementStep = notifySettlementStep;
    }

    public String settle(String claimId, String policyId, String category,
                         BigDecimal assessedAmount) {
        var context = new SagaContext();
        context.put("claimId", claimId);
        context.put("policyId", policyId);
        context.put("category", category);
        context.put("assessedAmount", assessedAmount.toPlainString());

        List<SagaStep> steps = List.of(
                fraudCheckStep,
                approveClaimStep,
                updatePolicyStep,
                notifySettlementStep
        );

        return orchestrator.execute("CLAIM_SETTLEMENT", steps, context);
    }
}
