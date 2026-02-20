package com.mongodb.course.m11.banking.model;

import java.math.BigDecimal;

public sealed interface FinancialProduct permits Deposit, Fund, InsuranceProduct {
    String id();
    String name();
    BigDecimal value();
}
