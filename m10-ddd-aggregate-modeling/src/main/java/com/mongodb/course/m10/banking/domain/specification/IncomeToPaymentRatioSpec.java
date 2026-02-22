package com.mongodb.course.m10.banking.domain.specification;

import com.mongodb.course.m10.banking.domain.model.Money;

/**
 * Specification: applicant's annual income must be at least {@code ratio} times the annual payment.
 */
public class IncomeToPaymentRatioSpec {

    private final int ratio;

    public IncomeToPaymentRatioSpec(int ratio) {
        if (ratio <= 0) {
            throw new IllegalArgumentException("Ratio must be positive");
        }
        this.ratio = ratio;
    }

    public boolean isSatisfiedBy(Money annualIncome, Money annualPayment) {
        Money threshold = annualPayment.multiply(ratio);
        return annualIncome.isGreaterThanOrEqual(threshold);
    }

    public int getRatio() {
        return ratio;
    }
}
