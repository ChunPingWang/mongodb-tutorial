package com.mongodb.course.m07;

import com.mongodb.course.m07.banking.*;
import com.mongodb.course.m07.ecommerce.Product;
import com.mongodb.course.m07.insurance.*;
import com.mongodb.course.m07.service.BucketAggregationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LAB-05: $bucket / $bucketAuto — 分桶分析
 */
@SpringBootTest
@Import(SharedContainersConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BucketAggregationTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private BucketAggregationService service;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(BankAccount.class);
        mongoTemplate.dropCollection(InsurancePolicyDocument.class);
        mongoTemplate.dropCollection(Product.class);

        // Banking: 6 accounts with varied balances
        BankAccount a1 = new BankAccount("ACC-001", "Alice", AccountType.SAVINGS, new BigDecimal("5000"));
        BankAccount a2 = new BankAccount("ACC-002", "Bob", AccountType.CHECKING, new BigDecimal("15000"));
        BankAccount a3 = new BankAccount("ACC-003", "Charlie", AccountType.SAVINGS, new BigDecimal("35000"));
        BankAccount a4 = new BankAccount("ACC-004", "David", AccountType.CHECKING, new BigDecimal("55000"));
        BankAccount a5 = new BankAccount("ACC-005", "Eve", AccountType.SAVINGS, new BigDecimal("75000"));
        BankAccount a6 = new BankAccount("ACC-006", "Frank", AccountType.CHECKING, new BigDecimal("120000"));
        mongoTemplate.insertAll(List.of(a1, a2, a3, a4, a5, a6));

        // Insurance: 6 policies
        LocalDate now = LocalDate.of(2025, 6, 1);
        InsurancePolicyDocument p1 = new InsurancePolicyDocument("POL-001", "Alice", PolicyType.TERM_LIFE,
                new BigDecimal("500"), new BigDecimal("100000"), now.minusYears(1), now.plusYears(1));
        InsurancePolicyDocument p2 = new InsurancePolicyDocument("POL-002", "Bob", PolicyType.HEALTH,
                new BigDecimal("800"), new BigDecimal("200000"), now.minusYears(1), now.plusYears(1));
        InsurancePolicyDocument p3 = new InsurancePolicyDocument("POL-003", "Charlie", PolicyType.WHOLE_LIFE,
                new BigDecimal("1200"), new BigDecimal("500000"), now.minusYears(1), now.plusYears(1));
        InsurancePolicyDocument p4 = new InsurancePolicyDocument("POL-004", "David", PolicyType.AUTO,
                new BigDecimal("600"), new BigDecimal("150000"), now.minusYears(1), now.plusYears(1));
        InsurancePolicyDocument p5 = new InsurancePolicyDocument("POL-005", "Eve", PolicyType.TERM_LIFE,
                new BigDecimal("1500"), new BigDecimal("700000"), now.minusYears(1), now.plusYears(1));
        InsurancePolicyDocument p6 = new InsurancePolicyDocument("POL-006", "Frank", PolicyType.HEALTH,
                new BigDecimal("900"), new BigDecimal("300000"), now.minusYears(1), now.plusYears(1));
        mongoTemplate.insertAll(List.of(p1, p2, p3, p4, p5, p6));

        // Products: 6 products with varied prices and ratings
        Product pr1 = new Product("SKU-001", "Mouse", "Electronics", new BigDecimal("25.00"), true);
        pr1.setRating(3.5);
        Product pr2 = new Product("SKU-002", "Keyboard", "Electronics", new BigDecimal("75.00"), true);
        pr2.setRating(4.5);
        Product pr3 = new Product("SKU-003", "Monitor", "Electronics", new BigDecimal("350.00"), true);
        pr3.setRating(4.8);
        Product pr4 = new Product("SKU-004", "Book A", "Books", new BigDecimal("30.00"), true);
        pr4.setRating(4.0);
        Product pr5 = new Product("SKU-005", "Book B", "Books", new BigDecimal("45.00"), true);
        pr5.setRating(2.5);
        Product pr6 = new Product("SKU-006", "Desk", "Furniture", new BigDecimal("250.00"), true);
        pr6.setRating(4.2);
        mongoTemplate.insertAll(List.of(pr1, pr2, pr3, pr4, pr5, pr6));
    }

    @Test
    @Order(1)
    void bucket_balanceDistribution() {
        // Boundaries: 0, 20000, 50000, 100000, MAX
        List<Map> results = service.balanceDistribution();
        assertThat(results).isNotEmpty();

        // [0, 20000): 5000, 15000 → 2 accounts
        Map firstBucket = results.stream()
                .filter(r -> {
                    Number id = (Number) r.get("_id");
                    return id.intValue() == 0;
                }).findFirst().orElseThrow();
        assertThat(((Number) firstBucket.get("count")).intValue()).isEqualTo(2);
    }

    @Test
    @Order(2)
    void bucketAuto_premiumDistribution() {
        List<Map> results = service.premiumBucketAuto(3);
        assertThat(results).hasSize(3);

        // Each bucket should have count > 0
        results.forEach(r -> {
            int count = ((Number) r.get("count")).intValue();
            assertThat(count).isGreaterThan(0);
        });
    }

    @Test
    @Order(3)
    void bucket_priceTiers() {
        // Boundaries: 0, 50, 100, 200, 500, MAX
        List<Map> results = service.priceTierDistribution();
        assertThat(results).isNotEmpty();

        // [0, 50): 25.00, 30.00, 45.00 → 3 products
        Map firstTier = results.stream()
                .filter(r -> {
                    Number id = (Number) r.get("_id");
                    return id.intValue() == 0;
                }).findFirst().orElseThrow();
        assertThat(((Number) firstTier.get("count")).intValue()).isEqualTo(3);
    }

    @Test
    @Order(4)
    void bucket_ratingDistribution() {
        // Boundaries: 1.0, 2.0, 3.0, 4.0, 5.01
        List<Map> results = service.ratingDistribution();
        assertThat(results).isNotEmpty();

        // [4.0, 5.01): ratings 4.0, 4.2, 4.5, 4.8 → 4 products
        Map highRating = results.stream()
                .filter(r -> {
                    Number id = (Number) r.get("_id");
                    return id.doubleValue() == 4.0;
                }).findFirst().orElseThrow();
        assertThat(((Number) highRating.get("count")).intValue()).isEqualTo(4);
    }

    @Test
    @Order(5)
    void bucketWithAccumulator_avgInBucket() {
        // Boundaries: 0, 30000, 60000, MAX
        List<Map> results = service.bucketWithAccumulator();
        assertThat(results).isNotEmpty();

        // Each bucket should have count, avgBalance, totalBalance
        results.forEach(r -> {
            assertThat(r).containsKeys("count", "avgBalance", "totalBalance");
        });

        // [0, 30000): 5000, 15000 → avg = 10000
        Map firstBucket = results.stream()
                .filter(r -> {
                    Number id = (Number) r.get("_id");
                    return id.intValue() == 0;
                }).findFirst().orElseThrow();
        assertThat(((Number) firstBucket.get("count")).intValue()).isEqualTo(2);
    }

    @Test
    @Order(6)
    void bucketAuto_granularity() {
        List<Map> results = service.bucketAutoGranularity(3);
        assertThat(results).isNotEmpty();

        // POWERSOF2 granularity: boundaries should be powers of 2
        results.forEach(r -> {
            assertThat(r).containsKey("count");
            int count = ((Number) r.get("count")).intValue();
            assertThat(count).isGreaterThan(0);
        });
    }
}
