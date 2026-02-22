package com.mongodb.course.m10.insurance.domain.model;

public record ClaimantReference(String claimantId) {

    public ClaimantReference {
        if (claimantId == null || claimantId.isBlank()) {
            throw new IllegalArgumentException("Claimant ID cannot be blank");
        }
    }
}
