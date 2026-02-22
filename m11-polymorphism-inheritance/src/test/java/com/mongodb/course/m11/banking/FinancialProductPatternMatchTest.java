package com.mongodb.course.m11.banking;

import com.mongodb.course.m11.banking.model.Deposit;
import com.mongodb.course.m11.banking.model.FinancialProduct;
import com.mongodb.course.m11.banking.model.Fund;
import com.mongodb.course.m11.banking.model.InsuranceProduct;
import com.mongodb.course.m11.banking.model.RiskProfile;
import com.mongodb.course.m11.banking.service.FinancialProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FinancialProductPatternMatchTest {

    private FinancialProductService service;

    @BeforeEach
    void setUp() {
        service = new FinancialProductService(mock(MongoTemplate.class));
    }

    @Test
    void estimateReturn_deposit_interestCalculation() {
        var deposit = new Deposit("d1", "一年定存", BigDecimal.valueOf(100000), BigDecimal.valueOf(2.0), 12);

        BigDecimal result = service.estimateAnnualReturn(deposit);

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(2000));
    }

    @Test
    void estimateReturn_fund_assumedReturn() {
        var fund = new Fund("f1", "全球股票", BigDecimal.valueOf(50000),
                BigDecimal.valueOf(15.32), new RiskProfile(5, "AGGRESSIVE"));

        BigDecimal result = service.estimateAnnualReturn(fund);

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(4000));
    }

    @Test
    void estimateReturn_insuranceProduct_coverageDividedByYears() {
        var insurance = new InsuranceProduct("i1", "年金險", BigDecimal.valueOf(200000),
                20, BigDecimal.valueOf(1000000));

        BigDecimal result = service.estimateAnnualReturn(insurance);

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(50000));
    }

    @Test
    void estimateReturn_allTypes_exhaustiveSwitch() {
        FinancialProduct[] products = {
                new Deposit("d1", "定存", BigDecimal.valueOf(100000), BigDecimal.valueOf(1.5), 12),
                new Fund("f1", "基金", BigDecimal.valueOf(50000),
                        BigDecimal.valueOf(10.0), new RiskProfile(3, "MODERATE")),
                new InsuranceProduct("i1", "保險", BigDecimal.valueOf(200000),
                        20, BigDecimal.valueOf(1000000))
        };

        for (FinancialProduct product : products) {
            BigDecimal result = service.estimateAnnualReturn(product);
            assertThat(result).isNotNull().isPositive();
        }
    }
}
