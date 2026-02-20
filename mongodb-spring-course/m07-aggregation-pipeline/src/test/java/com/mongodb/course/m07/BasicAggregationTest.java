package com.mongodb.course.m07;

import com.mongodb.course.m07.banking.*;
import com.mongodb.course.m07.dto.CategoryStats;
import com.mongodb.course.m07.dto.TypeCount;
import com.mongodb.course.m07.dto.TypeStats;
import com.mongodb.course.m07.dto.TypeSum;
import com.mongodb.course.m07.ecommerce.Product;
import com.mongodb.course.m07.service.BasicAggregationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LAB-01: Aggregation Pipeline 基礎 — match/group/sort/project/limit/count
 */
@SpringBootTest
@Import(SharedContainersConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BasicAggregationTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private BasicAggregationService service;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(BankAccount.class);
        mongoTemplate.dropCollection(Product.class);

        // Banking data: 5 accounts
        BankAccount a1 = new BankAccount("ACC-001", "Alice", AccountType.SAVINGS, new BigDecimal("50000"));
        a1.setStatus(AccountStatus.ACTIVE);
        BankAccount a2 = new BankAccount("ACC-002", "Bob", AccountType.CHECKING, new BigDecimal("15000"));
        a2.setStatus(AccountStatus.ACTIVE);
        BankAccount a3 = new BankAccount("ACC-003", "Charlie", AccountType.SAVINGS, new BigDecimal("80000"));
        a3.setStatus(AccountStatus.ACTIVE);
        BankAccount a4 = new BankAccount("ACC-004", "David", AccountType.CHECKING, new BigDecimal("25000"));
        a4.setStatus(AccountStatus.CLOSED);
        BankAccount a5 = new BankAccount("ACC-005", "Eve", AccountType.SAVINGS, new BigDecimal("30000"));
        a5.setStatus(AccountStatus.ACTIVE);
        mongoTemplate.insertAll(List.of(a1, a2, a3, a4, a5));

        // Product data: 6 products across 3 categories
        Product p1 = new Product("SKU-001", "Wireless Mouse", "Electronics", new BigDecimal("29.99"), true);
        p1.setRating(4.5);
        p1.setTags(List.of("wireless", "mouse", "computer"));
        Product p2 = new Product("SKU-002", "Mechanical Keyboard", "Electronics", new BigDecimal("89.99"), true);
        p2.setRating(4.8);
        p2.setTags(List.of("keyboard", "mechanical", "computer"));
        Product p3 = new Product("SKU-003", "Java Programming", "Books", new BigDecimal("45.00"), true);
        p3.setRating(4.2);
        p3.setTags(List.of("java", "programming"));
        Product p4 = new Product("SKU-004", "Spring Boot in Action", "Books", new BigDecimal("55.00"), true);
        p4.setRating(4.6);
        p4.setTags(List.of("spring", "java", "programming"));
        Product p5 = new Product("SKU-005", "USB-C Hub", "Electronics", new BigDecimal("39.99"), false);
        p5.setRating(3.9);
        p5.setTags(List.of("usb", "hub", "computer"));
        Product p6 = new Product("SKU-006", "Standing Desk", "Furniture", new BigDecimal("299.99"), true);
        p6.setRating(4.7);
        p6.setTags(List.of("desk", "ergonomic"));
        mongoTemplate.insertAll(List.of(p1, p2, p3, p4, p5, p6));
    }

    @Test
    @Order(1)
    void matchAndGroupCount_byType() {
        List<TypeCount> results = service.countByType(AccountStatus.ACTIVE);
        assertThat(results).hasSize(2);

        TypeCount savings = results.stream().filter(r -> "SAVINGS".equals(r.type())).findFirst().orElseThrow();
        TypeCount checking = results.stream().filter(r -> "CHECKING".equals(r.type())).findFirst().orElseThrow();
        assertThat(savings.count()).isEqualTo(3); // Alice, Charlie, Eve
        assertThat(checking.count()).isEqualTo(1); // Bob
    }

    @Test
    @Order(2)
    void groupSum_balanceByType() {
        List<TypeSum> results = service.sumBalanceByType();
        assertThat(results).hasSize(2);

        TypeSum savings = results.stream().filter(r -> "SAVINGS".equals(r.type())).findFirst().orElseThrow();
        TypeSum checking = results.stream().filter(r -> "CHECKING".equals(r.type())).findFirst().orElseThrow();
        // SAVINGS: 50000 + 80000 + 30000 = 160000
        assertThat(savings.total()).isEqualByComparingTo(new BigDecimal("160000"));
        // CHECKING: 15000 + 25000 = 40000
        assertThat(checking.total()).isEqualByComparingTo(new BigDecimal("40000"));
    }

    @Test
    @Order(3)
    void groupAvg_averageBalanceByType() {
        List<Map> results = service.averageBalanceByType();
        assertThat(results).hasSize(2);

        Map savings = results.stream()
                .filter(r -> "SAVINGS".equals(r.get("type"))).findFirst().orElseThrow();
        // avg SAVINGS: (50000+80000+30000)/3 ≈ 53333.33
        double avgSavings = ((Number) savings.get("avgBalance")).doubleValue();
        assertThat(avgSavings).isBetween(53333.0, 53334.0);
    }

    @Test
    @Order(4)
    void multipleAccumulators_statsPerType() {
        List<TypeStats> results = service.statsPerType();
        assertThat(results).hasSize(2);

        TypeStats savings = results.stream().filter(r -> "SAVINGS".equals(r.type())).findFirst().orElseThrow();
        assertThat(savings.count()).isEqualTo(3);
        assertThat(savings.total()).isEqualByComparingTo(new BigDecimal("160000"));
        assertThat(savings.min()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(savings.max()).isEqualByComparingTo(new BigDecimal("80000"));
    }

    @Test
    @Order(5)
    void sortAfterGroup_categoriesByCount() {
        List<CategoryStats> results = service.sortedCategoryCounts();
        assertThat(results).hasSize(3);
        // Electronics: 3, Books: 2, Furniture: 1
        assertThat(results.getFirst().category()).isEqualTo("Electronics");
        assertThat(results.getFirst().count()).isEqualTo(3);
        assertThat(results.getLast().category()).isEqualTo("Furniture");
    }

    @Test
    @Order(6)
    void limitAfterSort_topNCategories() {
        List<CategoryStats> results = service.topNCategories(2);
        assertThat(results).hasSize(2);
        assertThat(results.getFirst().category()).isEqualTo("Electronics");
        assertThat(results.get(1).category()).isEqualTo("Books");
    }

    @Test
    @Order(7)
    void projectComputedField_balanceTier() {
        List<Map> results = service.projectComputedField();
        assertThat(results).hasSize(4); // 4 ACTIVE accounts

        Map alice = results.stream()
                .filter(r -> "Alice".equals(r.get("holderName"))).findFirst().orElseThrow();
        assertThat(alice.get("balanceTier")).isEqualTo("HIGH"); // 50000 >= 50000

        Map bob = results.stream()
                .filter(r -> "Bob".equals(r.get("holderName"))).findFirst().orElseThrow();
        assertThat(bob.get("balanceTier")).isEqualTo("NORMAL"); // 15000 < 50000
    }

    @Test
    @Order(8)
    void countStage_totalActiveAccounts() {
        long count = service.countDocuments(AccountStatus.ACTIVE);
        assertThat(count).isEqualTo(4); // Alice, Bob, Charlie, Eve
    }
}
