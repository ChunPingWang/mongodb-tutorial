package com.mongodb.course.m10.insurance.domain.model;

public record PolicyReference(String policyId) {

    public PolicyReference {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("Policy ID cannot be blank");
        }
    }
}
