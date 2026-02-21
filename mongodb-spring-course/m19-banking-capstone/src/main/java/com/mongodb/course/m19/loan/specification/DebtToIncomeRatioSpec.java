package com.mongodb.course.m19.loan.specification;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class DebtToIncomeRatioSpec {

    private static final BigDecimal ANNUAL_RATE = new BigDecimal("0.025");
    private static final int INCOME_RATIO = 3;

    public boolean isSatisfiedBy(BigDecimal annualIncome, BigDecimal loanAmount, int termMonths) {
        BigDecimal annualPayment = calculateAnnualPayment(loanAmount, termMonths);
        BigDecimal requiredIncome = annualPayment.multiply(BigDecimal.valueOf(INCOME_RATIO));
        return annualIncome.compareTo(requiredIncome) >= 0;
    }

    private BigDecimal calculateAnnualPayment(BigDecimal principal, int termMonths) {
        int termYears = termMonths / 12;
        BigDecimal r = ANNUAL_RATE;
        BigDecimal onePlusR = BigDecimal.ONE.add(r);
        BigDecimal onePlusRPowN = onePlusR.pow(termYears, MathContext.DECIMAL128);
        BigDecimal numerator = principal.multiply(r).multiply(onePlusRPowN);
        BigDecimal denominator = onePlusRPowN.subtract(BigDecimal.ONE);
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
