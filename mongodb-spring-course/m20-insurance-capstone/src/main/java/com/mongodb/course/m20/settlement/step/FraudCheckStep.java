package com.mongodb.course.m20.settlement.step;

import com.mongodb.course.m20.infrastructure.saga.SagaContext;
import com.mongodb.course.m20.infrastructure.saga.SagaStep;
import com.mongodb.course.m20.projection.ClaimQueryService;
import org.springframework.stereotype.Component;

@Component
public class FraudCheckStep implements SagaStep {

    private final ClaimQueryService claimQueryService;

    public FraudCheckStep(ClaimQueryService claimQueryService) {
        this.claimQueryService = claimQueryService;
    }

    @Override
    public String name() {
        return "FRAUD_CHECK";
    }

    @Override
    public void execute(SagaContext context) {
        String claimId = context.get("claimId", String.class);
        String category = context.get("category", String.class);

        // Check claim's fraud risk from dashboard
        var dashboard = claimQueryService.findDashboardByClaimId(claimId)
                .orElseThrow(() -> new IllegalStateException("Claim dashboard not found: " + claimId));

        if ("HIGH".equals(dashboard.fraudRisk())) {
            throw new IllegalStateException("Fraud check failed: claim has HIGH fraud risk");
        }

        // Check category-level fraud pattern from statistics
        var stats = claimQueryService.findStatisticsByCategory(category);
        if (stats.isPresent()) {
            var s = stats.get();
            if (s.filedCount() > 5) {
                double approvalRate = s.filedCount() > 0
                        ? (double) s.approvedCount() / s.filedCount()
                        : 1.0;
                if (approvalRate < 0.3) {
                    throw new IllegalStateException(
                            "Fraud check failed: category " + category
                                    + " has suspicious pattern (filedCount=" + s.filedCount()
                                    + ", approvalRate=" + String.format("%.2f", approvalRate) + ")");
                }
            }
        }

        context.put("fraudCheckPassed", true);
    }

    @Override
    public void compensate(SagaContext context) {
        // Read-only step, no compensation needed
    }
}
