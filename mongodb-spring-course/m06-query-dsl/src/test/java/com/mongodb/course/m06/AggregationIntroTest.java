package com.mongodb.course.m06;

import com.mongodb.course.m06.banking.*;
import com.mongodb.course.m06.insurance.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

/**
 * LAB-06: Aggregation 入門 — Banking + Insurance
 * （深入 Aggregation Pipeline 請見 M07）
 */
@SpringBootTest
@Import(SharedContainersConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AggregationIntroTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(BankAccount.class);
        mongoTemplate.dropCollection(InsurancePolicyDocument.class);

        // Banking data
        BankAccount a1 = new BankAccount("ACC-001", "Alice", AccountType.SAVINGS, new BigDecimal("50000"));
        a1.setStatus(AccountStatus.ACTIVE);
        BankAccount a2 = new BankAccount("ACC-002", "Bob", AccountType.CHECKING, new BigDecimal("15000"));
        a2.setStatus(AccountStatus.ACTIVE);
        BankAccount a3 = new BankAccount("ACC-003", "Charlie", AccountType.SAVINGS, new BigDecimal("80000"));
        a3.setStatus(AccountStatus.ACTIVE);
        BankAccount a4 = new BankAccount("ACC-004", "David", AccountType.CHECKING, new BigDecimal("25000"));
        a4.setStatus(AccountStatus.CLOSED);
        mongoTemplate.insertAll(List.of(a1, a2, a3, a4));

        // Insurance data
        LocalDate now = LocalDate.of(2025, 6, 1);
        InsurancePolicyDocument p1 = new InsurancePolicyDocument("POL-001", "Alice", PolicyType.TERM_LIFE,
                new BigDecimal("1200"), new BigDecimal("500000"), now.minusYears(1), now.plusYears(1));
        InsurancePolicyDocument p2 = new InsurancePolicyDocument("POL-002", "Bob", PolicyType.HEALTH,
                new BigDecimal("800"), new BigDecimal("200000"), now.minusYears(1), now.plusYears(1));
        InsurancePolicyDocument p3 = new InsurancePolicyDocument("POL-003", "Charlie", PolicyType.TERM_LIFE,
                new BigDecimal("1500"), new BigDecimal("700000"), now.minusYears(1), now.plusYears(1));
        InsurancePolicyDocument p4 = new InsurancePolicyDocument("POL-004", "David", PolicyType.HEALTH,
                new BigDecimal("900"), new BigDecimal("300000"), now.minusYears(1), now.plusYears(1));
        mongoTemplate.insertAll(List.of(p1, p2, p3, p4));
    }

    @Test
    @Order(1)
    void basicAggregation_countByType() {
        // match ACTIVE + group by type + count
        Aggregation aggregation = newAggregation(
                match(Criteria.where("status").is(AccountStatus.ACTIVE)),
                group("type").count().as("count")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m06_bank_accounts", Map.class);

        List<Map> mappedResults = results.getMappedResults();
        assertThat(mappedResults).hasSize(2); // SAVINGS, CHECKING

        // Verify counts
        mappedResults.forEach(r -> {
            String type = (String) r.get("_id");
            int count = ((Number) r.get("count")).intValue();
            if ("SAVINGS".equals(type)) {
                assertThat(count).isEqualTo(2); // Alice, Charlie
            } else if ("CHECKING".equals(type)) {
                assertThat(count).isEqualTo(1); // Bob (David is CLOSED)
            }
        });
    }

    @Test
    @Order(2)
    void basicAggregation_sumPremiumByType() {
        // group by policyType + sum premium
        Aggregation aggregation = newAggregation(
                group("policyType").sum("premium").as("totalPremium")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m06_insurance_policies", Map.class);

        List<Map> mappedResults = results.getMappedResults();
        assertThat(mappedResults).hasSize(2); // TERM_LIFE, HEALTH

        mappedResults.forEach(r -> {
            String type = (String) r.get("_id");
            // Decimal128 sum comes back as org.bson.types.Decimal128
            BigDecimal total = new BigDecimal(r.get("totalPremium").toString());
            if ("TERM_LIFE".equals(type)) {
                assertThat(total).isEqualByComparingTo(new BigDecimal("2700")); // 1200 + 1500
            } else if ("HEALTH".equals(type)) {
                assertThat(total).isEqualByComparingTo(new BigDecimal("1700")); // 800 + 900
            }
        });
    }
}
