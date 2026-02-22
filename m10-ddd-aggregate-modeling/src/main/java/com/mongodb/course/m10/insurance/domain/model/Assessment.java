package com.mongodb.course.m10.insurance.domain.model;

import com.mongodb.course.m10.banking.domain.model.Money;

public record Assessment(String assessorName, Money approvedAmount, String notes) {

    public Assessment {
        if (assessorName == null || assessorName.isBlank()) {
            throw new IllegalArgumentException("Assessor name cannot be blank");
        }
        if (approvedAmount == null) {
            throw new IllegalArgumentException("Approved amount cannot be null");
        }
    }
}
