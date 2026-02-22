package com.mongodb.course.m10.banking.domain.model;

public record Applicant(String name, String nationalId, Money annualIncome, String employer) {

    public Applicant {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Applicant name cannot be blank");
        }
        if (nationalId == null || nationalId.isBlank()) {
            throw new IllegalArgumentException("National ID cannot be blank");
        }
        if (annualIncome == null) {
            throw new IllegalArgumentException("Annual income cannot be null");
        }
        if (employer == null || employer.isBlank()) {
            throw new IllegalArgumentException("Employer cannot be blank");
        }
    }
}
