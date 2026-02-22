package com.mongodb.course.m10.banking.domain;

import com.mongodb.course.m10.banking.domain.model.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void create_withNegativeAmount_throwsException() {
        assertThatThrownBy(() -> Money.twd(-100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    @Test
    void add_combinesAmounts() {
        Money a = Money.twd(1000);
        Money b = Money.twd(2500);

        Money result = a.add(b);

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("3500"));
        assertThat(result.currency()).isEqualTo("TWD");
    }

    @Test
    void multiply_scalesAmount() {
        Money money = Money.twd(500);

        Money result = money.multiply(3);

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("1500"));
    }
}
