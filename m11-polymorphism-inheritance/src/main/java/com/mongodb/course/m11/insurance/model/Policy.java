package com.mongodb.course.m11.insurance.model;

import java.math.BigDecimal;

public sealed interface Policy permits AutoPolicy, LifePolicy, HealthPolicy {
    String getId();
    String getPolicyNumber();
    String getHolderName();
    BigDecimal getBasePremium();
}
