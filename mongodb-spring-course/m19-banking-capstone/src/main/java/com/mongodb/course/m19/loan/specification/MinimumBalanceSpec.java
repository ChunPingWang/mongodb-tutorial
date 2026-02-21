package com.mongodb.course.m19.loan.specification;

import java.math.BigDecimal;

public class MinimumBalanceSpec {

    private static final BigDecimal MINIMUM_RATIO = new BigDecimal("0.1");

    public boolean isSatisfiedBy(BigDecimal currentBalance, BigDecimal requestedAmount) {
        BigDecimal minimumRequired = requestedAmount.multiply(MINIMUM_RATIO);
        return currentBalance.compareTo(minimumRequired) >= 0;
    }
}
