package com.mongodb.course.m10.banking.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record LoanTerm(int years, BigDecimal annualInterestRate) {

    public LoanTerm {
        if (years <= 0) {
            throw new IllegalArgumentException("Years must be positive");
        }
        if (annualInterestRate == null || annualInterestRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Annual interest rate must be positive");
        }
    }

    /**
     * Computes annual payment using the standard annuity formula:
     * Payment = P × r × (1+r)^n / ((1+r)^n - 1)
     * where P = principal, r = annual rate (decimal), n = years.
     */
    public Money computeAnnualPayment(Money principal) {
        BigDecimal r = annualInterestRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusR = BigDecimal.ONE.add(r);
        BigDecimal power = onePlusR.pow(years);
        BigDecimal numerator = principal.amount().multiply(r).multiply(power);
        BigDecimal denominator = power.subtract(BigDecimal.ONE);
        BigDecimal annualPayment = numerator.divide(denominator, 0, RoundingMode.HALF_UP);
        return new Money(annualPayment, principal.currency());
    }
}
