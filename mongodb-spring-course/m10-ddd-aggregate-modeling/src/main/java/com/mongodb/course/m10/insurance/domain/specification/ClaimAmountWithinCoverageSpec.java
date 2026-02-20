package com.mongodb.course.m10.insurance.domain.specification;

import com.mongodb.course.m10.banking.domain.model.Money;

import java.math.BigDecimal;

/**
 * Specification: approved amount must not exceed (totalClaimed - deductible).
 */
public class ClaimAmountWithinCoverageSpec {

    public boolean isSatisfiedBy(Money approvedAmount, Money totalClaimed, Money deductible) {
        BigDecimal maxApprovable = totalClaimed.amount().subtract(deductible.amount());
        return approvedAmount.amount().compareTo(maxApprovable) <= 0;
    }
}
