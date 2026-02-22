package com.mongodb.course.m10.banking.domain;

import com.mongodb.course.m10.banking.domain.model.Money;
import com.mongodb.course.m10.banking.domain.specification.IncomeToPaymentRatioSpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IncomeToPaymentRatioSpecTest {

    @Test
    void isSatisfied_incomeExceedsThreshold() {
        var spec = new IncomeToPaymentRatioSpec(3);
        Money income = Money.twd(400_000);
        Money payment = Money.twd(100_000);

        assertThat(spec.isSatisfiedBy(income, payment)).isTrue();
    }

    @Test
    void isNotSatisfied_incomeBelowThreshold() {
        var spec = new IncomeToPaymentRatioSpec(3);
        Money income = Money.twd(200_000);
        Money payment = Money.twd(100_000);

        assertThat(spec.isSatisfiedBy(income, payment)).isFalse();
    }
}
