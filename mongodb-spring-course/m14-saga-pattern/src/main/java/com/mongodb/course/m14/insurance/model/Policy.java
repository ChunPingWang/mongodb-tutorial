package com.mongodb.course.m14.insurance.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("m14_policies")
public record Policy(
        @Id String policyId,
        String holderName,
        long coverageAmount,
        long paidClaimsTotal
) {
}
