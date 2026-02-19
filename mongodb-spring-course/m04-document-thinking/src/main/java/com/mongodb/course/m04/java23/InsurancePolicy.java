package com.mongodb.course.m04.java23;

import org.springframework.data.annotation.Id;

import java.math.BigDecimal;

public record InsurancePolicy(
        @Id String id,
        String name,
        BigDecimal value,
        String policyType
) implements FinancialProduct {
}
