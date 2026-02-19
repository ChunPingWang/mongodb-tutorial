package com.mongodb.course.m04.java23;

import java.math.BigDecimal;

public sealed interface FinancialProduct permits SavingsAccount, FixedDeposit, InsurancePolicy {

    String id();

    String name();

    BigDecimal value();
}
