package com.mongodb.course.m20.underwriting.specification;

import com.mongodb.course.m20.underwriting.model.UnderwritingApplication;

public class AgeEligibilitySpec {

    public boolean isSatisfiedBy(UnderwritingApplication application) {
        int age = application.getApplicant().age();
        return switch (application.getRequestedPolicyType()) {
            case "AUTO" -> age >= 18 && age <= 75;
            case "HEALTH" -> age >= 0 && age <= 80;
            case "LIFE" -> age >= 18 && age <= 65;
            default -> false;
        };
    }
}
