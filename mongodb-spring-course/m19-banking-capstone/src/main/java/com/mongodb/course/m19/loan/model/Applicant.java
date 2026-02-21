package com.mongodb.course.m19.loan.model;

import java.math.BigDecimal;

public record Applicant(
        String name,
        String accountId,
        BigDecimal annualIncome
) {
}
