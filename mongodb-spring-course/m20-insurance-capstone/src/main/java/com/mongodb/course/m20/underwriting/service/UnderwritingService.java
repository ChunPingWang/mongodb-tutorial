package com.mongodb.course.m20.underwriting.service;

import com.mongodb.course.m20.policy.*;
import com.mongodb.course.m20.projection.ClaimQueryService;
import com.mongodb.course.m20.underwriting.model.PolicyApplicant;
import com.mongodb.course.m20.underwriting.model.UnderwritingApplication;
import com.mongodb.course.m20.underwriting.model.UnderwritingStatus;
import com.mongodb.course.m20.underwriting.specification.AgeEligibilitySpec;
import com.mongodb.course.m20.underwriting.specification.ClaimHistoryRiskSpec;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class UnderwritingService {

    private final PolicyService policyService;
    private final ClaimQueryService claimQueryService;

    private final AgeEligibilitySpec ageSpec = new AgeEligibilitySpec();
    private final ClaimHistoryRiskSpec riskSpec = new ClaimHistoryRiskSpec();

    public UnderwritingService(PolicyService policyService, ClaimQueryService claimQueryService) {
        this.policyService = policyService;
        this.claimQueryService = claimQueryService;
    }

    public UnderwritingApplication submit(PolicyApplicant applicant, String policyType,
                                           BigDecimal requestedCoverage) {
        return UnderwritingApplication.submit(applicant, policyType, requestedCoverage);
    }

    public UnderwritingApplication review(UnderwritingApplication application) {
        // Query CQRS read model for paid claims count
        int paidClaimsCount = claimQueryService.countPaidClaimsByCategory(
                application.getRequestedPolicyType());

        application.review(ageSpec, riskSpec, paidClaimsCount);

        if (application.getStatus() == UnderwritingStatus.APPROVED) {
            createPolicy(application);
        }

        return application;
    }

    private void createPolicy(UnderwritingApplication application) {
        String id = UUID.randomUUID().toString();
        String policyNumber = "POL-" + id.substring(0, 8).toUpperCase();
        String holderName = application.getApplicant().name();
        BigDecimal basePremium = application.getRequestedCoverage()
                .multiply(new BigDecimal("0.05"));
        BigDecimal coverage = application.getRequestedCoverage();

        Policy policy = switch (application.getRequestedPolicyType()) {
            case "AUTO" -> new AutoPolicy(id, policyNumber, holderName,
                    basePremium, coverage, "Sedan");
            case "HEALTH" -> new HealthPolicy(id, policyNumber, holderName,
                    basePremium, coverage, "STANDARD");
            case "LIFE" -> new LifePolicy(id, policyNumber, holderName,
                    basePremium, coverage, "Legal Heir", 20);
            default -> throw new IllegalArgumentException("Unknown policy type: "
                    + application.getRequestedPolicyType());
        };

        policyService.save(policy);
    }
}
