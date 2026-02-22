package com.mongodb.course.m20.underwriting.specification;

public class ClaimHistoryRiskSpec {

    private static final int MAX_PAID_CLAIMS = 3;

    public boolean isSatisfiedBy(int totalPaidClaimsCount) {
        return totalPaidClaimsCount <= MAX_PAID_CLAIMS;
    }
}
