package com.mongodb.course.m11.strategy;

import com.mongodb.course.m11.SharedContainersConfig;
import com.mongodb.course.m11.banking.model.Deposit;
import com.mongodb.course.m11.banking.model.FinancialProduct;
import com.mongodb.course.m11.banking.model.Fund;
import com.mongodb.course.m11.banking.model.InsuranceProduct;
import com.mongodb.course.m11.banking.model.RiskProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class StrategyComparisonTest {

    private static final String SINGLE_COLLECTION = "m11_strategy_single";
    private static final String DEPOSIT_COLLECTION = "m11_strategy_deposits";
    private static final String FUND_COLLECTION = "m11_strategy_funds";
    private static final String INSURANCE_COLLECTION = "m11_strategy_insurance";

    @Autowired
    private MongoTemplate mongoTemplate;

    private Deposit deposit;
    private Fund fund;
    private InsuranceProduct insurance;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(SINGLE_COLLECTION);
        mongoTemplate.dropCollection(DEPOSIT_COLLECTION);
        mongoTemplate.dropCollection(FUND_COLLECTION);
        mongoTemplate.dropCollection(INSURANCE_COLLECTION);

        deposit = new Deposit(null, "定存", BigDecimal.valueOf(100000),
                BigDecimal.valueOf(1.5), 12);
        fund = new Fund(null, "基金", BigDecimal.valueOf(50000),
                BigDecimal.valueOf(15.0), new RiskProfile(3, "MODERATE"));
        insurance = new InsuranceProduct(null, "保險", BigDecimal.valueOf(200000),
                20, BigDecimal.valueOf(1000000));
    }

    @Test
    void singleCollection_allTypesInOneCollection() {
        // Single Collection Strategy: all types in one collection
        mongoTemplate.save(deposit, SINGLE_COLLECTION);
        mongoTemplate.save(fund, SINGLE_COLLECTION);
        mongoTemplate.save(insurance, SINGLE_COLLECTION);

        List<FinancialProduct> all = mongoTemplate.findAll(FinancialProduct.class, SINGLE_COLLECTION);
        assertThat(all).hasSize(3);
        assertThat(all).hasAtLeastOneElementOfType(Deposit.class);
        assertThat(all).hasAtLeastOneElementOfType(Fund.class);
        assertThat(all).hasAtLeastOneElementOfType(InsuranceProduct.class);

        // Verify single collection has 3 documents
        long count = mongoTemplate.getCollection(SINGLE_COLLECTION).countDocuments();
        assertThat(count).isEqualTo(3);
    }

    @Test
    void multipleCollections_separateCollectionsPerType() {
        // Multiple Collection Strategy: one collection per type
        mongoTemplate.save(deposit, DEPOSIT_COLLECTION);
        mongoTemplate.save(fund, FUND_COLLECTION);
        mongoTemplate.save(insurance, INSURANCE_COLLECTION);

        // Each collection has exactly 1 document
        assertThat(mongoTemplate.getCollection(DEPOSIT_COLLECTION).countDocuments()).isEqualTo(1);
        assertThat(mongoTemplate.getCollection(FUND_COLLECTION).countDocuments()).isEqualTo(1);
        assertThat(mongoTemplate.getCollection(INSURANCE_COLLECTION).countDocuments()).isEqualTo(1);

        // Can query each type independently without _class filter
        List<Deposit> deposits = mongoTemplate.findAll(Deposit.class, DEPOSIT_COLLECTION);
        assertThat(deposits).hasSize(1);
        assertThat(deposits.getFirst().name()).isEqualTo("定存");
    }
}
