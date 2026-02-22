package com.mongodb.course.m07;

import com.mongodb.course.m07.banking.*;
import com.mongodb.course.m07.dto.CategoryStats;
import com.mongodb.course.m07.dto.TypeStats;
import com.mongodb.course.m07.dto.TypeSum;
import com.mongodb.course.m07.ecommerce.OrderItem;
import com.mongodb.course.m07.ecommerce.OrderStatus;
import com.mongodb.course.m07.ecommerce.Product;
import com.mongodb.course.m07.insurance.*;
import com.mongodb.course.m07.service.GroupAccumulatorService;
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
 * LAB-02: Group 累加器 — sum/avg/min/max/push/addToSet/first/last
 */
@SpringBootTest
@Import(SharedContainersConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GroupAccumulatorTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private GroupAccumulatorService service;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(BankAccount.class);
        mongoTemplate.dropCollection(InsurancePolicyDocument.class);
        mongoTemplate.dropCollection(Product.class);
        mongoTemplate.dropCollection(com.mongodb.course.m07.ecommerce.Order.class);

        // Banking: 4 accounts
        BankAccount a1 = new BankAccount("ACC-001", "Alice", AccountType.SAVINGS, new BigDecimal("50000"));
        a1.setStatus(AccountStatus.ACTIVE);
        BankAccount a2 = new BankAccount("ACC-002", "Bob", AccountType.CHECKING, new BigDecimal("15000"));
        a2.setStatus(AccountStatus.ACTIVE);
        BankAccount a3 = new BankAccount("ACC-003", "Charlie", AccountType.SAVINGS, new BigDecimal("80000"));
        a3.setStatus(AccountStatus.ACTIVE);
        BankAccount a4 = new BankAccount("ACC-004", "David", AccountType.CHECKING, new BigDecimal("25000"));
        a4.setStatus(AccountStatus.CLOSED);
        mongoTemplate.insertAll(List.of(a1, a2, a3, a4));

        // Insurance: 4 policies
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

        // Products: 4 products
        Product pr1 = new Product("SKU-001", "Wireless Mouse", "Electronics", new BigDecimal("29.99"), true);
        pr1.setRating(4.5);
        pr1.setTags(List.of("wireless", "mouse"));
        Product pr2 = new Product("SKU-002", "Keyboard", "Electronics", new BigDecimal("89.99"), true);
        pr2.setRating(4.8);
        pr2.setTags(List.of("keyboard", "mechanical"));
        Product pr3 = new Product("SKU-003", "Java Book", "Books", new BigDecimal("45.00"), true);
        pr3.setRating(4.2);
        pr3.setTags(List.of("java", "programming"));
        Product pr4 = new Product("SKU-004", "Spring Book", "Books", new BigDecimal("55.00"), true);
        pr4.setRating(4.6);
        pr4.setTags(List.of("spring", "java"));
        mongoTemplate.insertAll(List.of(pr1, pr2, pr3, pr4));

        // Orders: 3 orders
        var o1 = new com.mongodb.course.m07.ecommerce.Order("ORD-001", "Alice", new BigDecimal("500"), OrderStatus.DELIVERED, LocalDate.of(2025, 1, 15));
        o1.setItems(List.of(new OrderItem("SKU-001", "Wireless Mouse", 2, new BigDecimal("29.99"))));
        var o2 = new com.mongodb.course.m07.ecommerce.Order("ORD-002", "Bob", new BigDecimal("300"), OrderStatus.SHIPPED, LocalDate.of(2025, 2, 10));
        o2.setItems(List.of(new OrderItem("SKU-002", "Keyboard", 1, new BigDecimal("89.99"))));
        var o3 = new com.mongodb.course.m07.ecommerce.Order("ORD-003", "Alice", new BigDecimal("200"), OrderStatus.DELIVERED, LocalDate.of(2025, 3, 5));
        o3.setItems(List.of(new OrderItem("SKU-003", "Java Book", 1, new BigDecimal("45.00"))));
        mongoTemplate.insertAll(List.of(o1, o2, o3));
    }

    @Test
    @Order(1)
    void premiumStats_byPolicyType() {
        List<TypeStats> results = service.premiumStatsByType();
        assertThat(results).hasSize(2);

        TypeStats termLife = results.stream()
                .filter(r -> "TERM_LIFE".equals(r.type())).findFirst().orElseThrow();
        assertThat(termLife.count()).isEqualTo(2);
        assertThat(termLife.total()).isEqualByComparingTo(new BigDecimal("2700")); // 1200+1500
        assertThat(termLife.min()).isEqualByComparingTo(new BigDecimal("1200"));
        assertThat(termLife.max()).isEqualByComparingTo(new BigDecimal("1500"));
    }

    @Test
    @Order(2)
    @SuppressWarnings("unchecked")
    void push_collectHolderNames() {
        List<Map> results = service.collectHolderNamesByType();
        assertThat(results).hasSize(2);

        Map savings = results.stream()
                .filter(r -> "SAVINGS".equals(r.get("type"))).findFirst().orElseThrow();
        List<String> names = (List<String>) savings.get("holderNames");
        assertThat(names).containsExactlyInAnyOrder("Alice", "Charlie");
    }

    @Test
    @Order(3)
    @SuppressWarnings("unchecked")
    void addToSet_distinctCategories() {
        List<Map> results = service.collectDistinctCategoriesByTag();
        assertThat(results).isNotEmpty();

        // "java" tag appears in both Books products
        Map javaTag = results.stream()
                .filter(r -> "java".equals(r.get("tag"))).findFirst().orElseThrow();
        List<String> categories = (List<String>) javaTag.get("categories");
        assertThat(categories).containsExactly("Books"); // addToSet deduplicates
    }

    @Test
    @Order(4)
    void firstLast_withPriorSort() {
        List<Map> results = service.firstAndLastByType();
        assertThat(results).hasSize(2);

        Map savings = results.stream()
                .filter(r -> "SAVINGS".equals(r.get("type"))).findFirst().orElseThrow();
        // Sorted by balance ASC: Alice(50000) < Charlie(80000)
        assertThat(savings.get("firstHolder")).isEqualTo("Alice");
        assertThat(savings.get("lastHolder")).isEqualTo("Charlie");
    }

    @Test
    @Order(5)
    void categoryStats_countAvgPriceRating() {
        List<CategoryStats> results = service.categoryStats();
        assertThat(results).hasSize(2); // Electronics, Books

        CategoryStats electronics = results.stream()
                .filter(r -> "Electronics".equals(r.category())).findFirst().orElseThrow();
        assertThat(electronics.count()).isEqualTo(2);
        assertThat(electronics.avgRating()).isBetween(4.6, 4.7); // (4.5+4.8)/2
    }

    @Test
    @Order(6)
    void totalRevenue_byOrderStatus() {
        List<TypeSum> results = service.totalRevenueByStatus();
        assertThat(results).hasSize(2); // DELIVERED, SHIPPED

        TypeSum delivered = results.stream()
                .filter(r -> "DELIVERED".equals(r.type())).findFirst().orElseThrow();
        assertThat(delivered.total()).isEqualByComparingTo(new BigDecimal("700")); // 500+200
    }

    @Test
    @Order(7)
    void groupByMultipleFields() {
        List<Map> results = service.groupByMultipleFields();
        assertThat(results).hasSize(3); // SAVINGS+ACTIVE, CHECKING+ACTIVE, CHECKING+CLOSED

        Map savingsActive = results.stream()
                .filter(r -> "SAVINGS".equals(r.get("type")) && "ACTIVE".equals(r.get("status")))
                .findFirst().orElseThrow();
        assertThat(((Number) savingsActive.get("count")).intValue()).isEqualTo(2);
    }

    @Test
    @Order(8)
    void groupNull_grandTotal() {
        Map result = service.grandTotal();
        assertThat(result).isNotEmpty();
        assertThat(((Number) result.get("totalAccounts")).intValue()).isEqualTo(4);
        // Total: 50000+15000+80000+25000 = 170000
        BigDecimal totalBalance = new BigDecimal(result.get("totalBalance").toString());
        assertThat(totalBalance).isEqualByComparingTo(new BigDecimal("170000"));
    }
}
