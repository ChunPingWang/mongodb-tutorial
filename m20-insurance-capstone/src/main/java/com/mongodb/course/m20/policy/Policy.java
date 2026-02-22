package com.mongodb.course.m20.policy;

import java.math.BigDecimal;

public sealed interface Policy permits AutoPolicy, HealthPolicy, LifePolicy {
    String getId();
    String getPolicyNumber();
    String getHolderName();
    BigDecimal getBasePremium();
    BigDecimal getCoverageAmount();
    BigDecimal getTotalClaimsPaid();
}
