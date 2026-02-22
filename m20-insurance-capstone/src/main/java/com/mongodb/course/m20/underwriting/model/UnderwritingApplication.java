package com.mongodb.course.m20.underwriting.model;

import java.math.BigDecimal;
import java.util.UUID;

public class UnderwritingApplication {

    private String applicationId;
    private PolicyApplicant applicant;
    private String requestedPolicyType;
    private BigDecimal requestedCoverage;
    private UnderwritingStatus status;
    private String rejectionReason;

    private UnderwritingApplication() {
    }

    public static UnderwritingApplication submit(PolicyApplicant applicant,
                                                  String policyType,
                                                  BigDecimal coverage) {
        var app = new UnderwritingApplication();
        app.applicationId = UUID.randomUUID().toString();
        app.applicant = applicant;
        app.requestedPolicyType = policyType;
        app.requestedCoverage = coverage;
        app.status = UnderwritingStatus.SUBMITTED;
        return app;
    }

    public void review(com.mongodb.course.m20.underwriting.specification.AgeEligibilitySpec ageSpec,
                       com.mongodb.course.m20.underwriting.specification.ClaimHistoryRiskSpec riskSpec,
                       int paidClaimsCount) {
        if (status != UnderwritingStatus.SUBMITTED) {
            throw new IllegalStateException("Can only review SUBMITTED applications, current: " + status);
        }

        this.status = UnderwritingStatus.UNDER_REVIEW;

        if (!ageSpec.isSatisfiedBy(this)) {
            this.status = UnderwritingStatus.REJECTED;
            this.rejectionReason = "Age ineligible for " + requestedPolicyType + " policy";
            return;
        }

        if (!riskSpec.isSatisfiedBy(paidClaimsCount)) {
            this.status = UnderwritingStatus.REJECTED;
            this.rejectionReason = "Too many paid claims in history: " + paidClaimsCount;
            return;
        }

        this.status = UnderwritingStatus.APPROVED;
    }

    public static UnderwritingApplication reconstitute(String applicationId,
                                                        PolicyApplicant applicant,
                                                        String requestedPolicyType,
                                                        BigDecimal requestedCoverage,
                                                        UnderwritingStatus status,
                                                        String rejectionReason) {
        var app = new UnderwritingApplication();
        app.applicationId = applicationId;
        app.applicant = applicant;
        app.requestedPolicyType = requestedPolicyType;
        app.requestedCoverage = requestedCoverage;
        app.status = status;
        app.rejectionReason = rejectionReason;
        return app;
    }

    public String getApplicationId() { return applicationId; }
    public PolicyApplicant getApplicant() { return applicant; }
    public String getRequestedPolicyType() { return requestedPolicyType; }
    public BigDecimal getRequestedCoverage() { return requestedCoverage; }
    public UnderwritingStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
}
