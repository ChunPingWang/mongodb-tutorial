package com.mongodb.course.m11.insurance;

import com.mongodb.course.m11.insurance.model.AutoPolicy;
import com.mongodb.course.m11.insurance.model.HealthPolicy;
import com.mongodb.course.m11.insurance.model.LifePolicy;
import com.mongodb.course.m11.insurance.service.PremiumCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PremiumCalculatorTest {

    private PremiumCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PremiumCalculator();
    }

    @Test
    void auto_youngDriver_surcharge() {
        var policy = new AutoPolicy(null, "POL-A01", "王小明",
                BigDecimal.valueOf(12000), "sedan", 22);

        BigDecimal premium = calculator.calculatePremium(policy);

        assertThat(premium).isEqualByComparingTo(BigDecimal.valueOf(18000));
    }

    @Test
    void auto_truck_surcharge() {
        var policy = new AutoPolicy(null, "POL-A02", "李大明",
                BigDecimal.valueOf(12000), "truck", 35);

        BigDecimal premium = calculator.calculatePremium(policy);

        assertThat(premium).isEqualByComparingTo(BigDecimal.valueOf(15600));
    }

    @Test
    void auto_default_basePremium() {
        var policy = new AutoPolicy(null, "POL-A03", "張三",
                BigDecimal.valueOf(12000), "sedan", 35);

        BigDecimal premium = calculator.calculatePremium(policy);

        assertThat(premium).isEqualByComparingTo(BigDecimal.valueOf(12000));
    }

    @Test
    void life_senior_doublePremium() {
        var policy = new LifePolicy(null, "POL-L01", "陳老伯",
                BigDecimal.valueOf(20000), 65, 20, BigDecimal.valueOf(5000000));

        BigDecimal premium = calculator.calculatePremium(policy);

        assertThat(premium).isEqualByComparingTo(BigDecimal.valueOf(40000));
    }

    @Test
    void life_normal_ageFactor() {
        // age 40: basePremium × (1 + 40 × 0.02) = 20000 × 1.8 = 36000
        var policy = new LifePolicy(null, "POL-L02", "李小花",
                BigDecimal.valueOf(20000), 40, 20, BigDecimal.valueOf(3000000));

        BigDecimal premium = calculator.calculatePremium(policy);

        assertThat(premium).isEqualByComparingTo(BigDecimal.valueOf(36000));
    }

    @Test
    void health_withOptions() {
        var policy = new HealthPolicy(null, "POL-H01", "張大山",
                BigDecimal.valueOf(8000), true, true, 5000);

        BigDecimal premium = calculator.calculatePremium(policy);

        assertThat(premium).isEqualByComparingTo(BigDecimal.valueOf(8800));
    }
}
