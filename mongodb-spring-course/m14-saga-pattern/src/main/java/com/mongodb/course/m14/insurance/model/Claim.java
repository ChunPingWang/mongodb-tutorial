package com.mongodb.course.m14.insurance.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("m14_claims")
public record Claim(
        @Id String claimId,
        String policyId,
        String claimantName,
        long amount,
        ClaimSettlementStatus settlementStatus
) {
}
