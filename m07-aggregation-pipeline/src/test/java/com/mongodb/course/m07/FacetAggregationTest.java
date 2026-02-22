package com.mongodb.course.m07;

import com.mongodb.course.m07.banking.*;
import com.mongodb.course.m07.ecommerce.OrderItem;
import com.mongodb.course.m07.ecommerce.OrderStatus;
import com.mongodb.course.m07.ecommerce.Product;
import com.mongodb.course.m07.insurance.*;
import com.mongodb.course.m07.service.FacetAggregationService;
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
 * LAB-06: $facet — 多面向平行查詢
 */
@SpringBootTest
@Import(SharedContainersConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FacetAggregationTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private FacetAggregationService service;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(Product.class);
        mongoTemplate.dropCollection(BankAccount.class);
        mongoTemplate.dropCollection(InsurancePolicyDocument.class);
        mongoTemplate.dropCollection(com.mongodb.course.m07.ecommerce.Order.class);

        // Products: 6 products
        Product p1 = new Product("SKU-001", "Wireless Mouse", "Electronics", new BigDecimal("29.99"), true);
        p1.setRating(4.5);
        Product p2 = new Product("SKU-002", "Keyboard", "Electronics", new BigDecimal("89.99"), true);
        p2.setRating(4.8);
        Product p3 = new Product("SKU-003", "Monitor", "Electronics", new BigDecimal("350.00"), false);
        p3.setRating(4.3);
        Product p4 = new Product("SKU-004", "Java Book", "Books", new BigDecimal("45.00"), true);
        p4.setRating(4.2);
        Product p5 = new Product("SKU-005", "Spring Book", "Books", new BigDecimal("55.00"), true);
        p5.setRating(4.6);
        Product p6 = new Product("SKU-006", "Standing Desk", "Furniture", new BigDecimal("299.99"), true);
        p6.setRating(4.7);
        mongoTemplate.insertAll(List.of(p1, p2, p3, p4, p5, p6));

        // Banking: 4 accounts
        BankAccount a1 = new BankAccount("ACC-001", "Alice", AccountType.SAVINGS, new BigDecimal("50000"));
        a1.setStatus(AccountStatus.ACTIVE);
        BankAccount a2 = new BankAccount("ACC-002", "Bob", AccountType.CHECKING, new BigDecimal("15000"));
        a2.setStatus(AccountStatus.ACTIVE);
        BankAccount a3 = new BankAccount("ACC-003", "Charlie", AccountType.SAVINGS, new BigDecimal("80000"));
        a3.setStatus(AccountStatus.CLOSED);
        BankAccount a4 = new BankAccount("ACC-004", "David", AccountType.CHECKING, new BigDecimal("25000"));
        a4.setStatus(AccountStatus.ACTIVE);
        mongoTemplate.insertAll(List.of(a1, a2, a3, a4));

        // Insurance: 4 policies
        LocalDate now = LocalDate.of(2025, 6, 1);
        InsurancePolicyDocument ip1 = new InsurancePolicyDocument("POL-001", "Alice", PolicyType.TERM_LIFE,
                new BigDecimal("1200"), new BigDecimal("500000"), now.minusYears(1), now.plusYears(1));
        InsurancePolicyDocument ip2 = new InsurancePolicyDocument("POL-002", "Bob", PolicyType.HEALTH,
                new BigDecimal("800"), new BigDecimal("200000"), now.minusYears(1), now.plusYears(1));
        InsurancePolicyDocument ip3 = new InsurancePolicyDocument("POL-003", "Charlie", PolicyType.TERM_LIFE,
                new BigDecimal("1500"), new BigDecimal("700000"), now.minusYears(1), now.plusYears(1));
        ip3.setStatus(PolicyStatus.EXPIRED);
        InsurancePolicyDocument ip4 = new InsurancePolicyDocument("POL-004", "David", PolicyType.HEALTH,
                new BigDecimal("900"), new BigDecimal("300000"), now.minusYears(1), now.plusYears(1));
        mongoTemplate.insertAll(List.of(ip1, ip2, ip3, ip4));

        // Orders: 4 orders
        var o1 = new com.mongodb.course.m07.ecommerce.Order("ORD-001", "Alice", new BigDecimal("500"), OrderStatus.DELIVERED, LocalDate.of(2025, 1, 15));
        o1.setItems(List.of(new OrderItem("SKU-001", "Mouse", 2, new BigDecimal("29.99"))));
        var o2 = new com.mongodb.course.m07.ecommerce.Order("ORD-002", "Bob", new BigDecimal("300"), OrderStatus.SHIPPED, LocalDate.of(2025, 2, 10));
        o2.setItems(List.of(new OrderItem("SKU-002", "Keyboard", 1, new BigDecimal("89.99"))));
        var o3 = new com.mongodb.course.m07.ecommerce.Order("ORD-003", "Alice", new BigDecimal("200"), OrderStatus.DELIVERED, LocalDate.of(2025, 3, 5));
        o3.setItems(List.of(new OrderItem("SKU-004", "Java Book", 1, new BigDecimal("45.00"))));
        var o4 = new com.mongodb.course.m07.ecommerce.Order("ORD-004", "Charlie", new BigDecimal("100"), OrderStatus.CANCELLED, LocalDate.of(2025, 4, 1));
        o4.setItems(List.of(new OrderItem("SKU-005", "Spring Book", 1, new BigDecimal("55.00"))));
        mongoTemplate.insertAll(List.of(o1, o2, o3, o4));
    }

    @Test
    @Order(1)
    @SuppressWarnings("unchecked")
    void productFacet_countDataStats() {
        Map result = service.productSearchFacet("Electronics");

        // totalCount facet
        List<Map> totalCount = (List<Map>) result.get("totalCount");
        assertThat(totalCount).hasSize(1);
        assertThat(((Number) totalCount.getFirst().get("total")).intValue()).isEqualTo(3);

        // data facet (paginated)
        List<Map> data = (List<Map>) result.get("data");
        assertThat(data).hasSize(3);

        // stats facet
        List<Map> stats = (List<Map>) result.get("stats");
        assertThat(stats).hasSize(1);
        assertThat(stats.getFirst()).containsKeys("avgPrice", "minPrice", "maxPrice");
    }

    @Test
    @Order(2)
    @SuppressWarnings("unchecked")
    void bankingDashboard_multipleViews() {
        Map result = service.bankingDashboard();

        // statusCounts
        List<Map> statusCounts = (List<Map>) result.get("statusCounts");
        assertThat(statusCounts).hasSize(2); // ACTIVE, CLOSED

        // typeCounts
        List<Map> typeCounts = (List<Map>) result.get("typeCounts");
        assertThat(typeCounts).hasSize(2); // SAVINGS, CHECKING

        // balanceStats
        List<Map> balanceStats = (List<Map>) result.get("balanceStats");
        assertThat(balanceStats).hasSize(1);
        assertThat(balanceStats.getFirst()).containsKeys("totalBalance", "avgBalance");
    }

    @Test
    @Order(3)
    @SuppressWarnings("unchecked")
    void insuranceOverview_threeFacets() {
        Map result = service.insuranceOverview();

        List<Map> byType = (List<Map>) result.get("byType");
        assertThat(byType).hasSize(2); // TERM_LIFE, HEALTH

        List<Map> byStatus = (List<Map>) result.get("byStatus");
        assertThat(byStatus).hasSize(2); // ACTIVE, EXPIRED

        List<Map> premiumRange = (List<Map>) result.get("premiumRange");
        assertThat(premiumRange).hasSize(1);
        assertThat(premiumRange.getFirst()).containsKeys("totalPremium", "avgPremium");
    }

    @Test
    @Order(4)
    @SuppressWarnings("unchecked")
    void orderAnalytics_statusAndTop() {
        Map result = service.orderAnalytics();

        List<Map> byStatus = (List<Map>) result.get("byStatus");
        assertThat(byStatus).hasSize(3); // DELIVERED, SHIPPED, CANCELLED

        List<Map> topCustomers = (List<Map>) result.get("topCustomers");
        assertThat(topCustomers).isNotEmpty();
        // Alice has 2 orders totaling 700
        Map top = topCustomers.getFirst();
        assertThat(top.get("customerName")).isEqualTo("Alice");
    }

    @Test
    @Order(5)
    @SuppressWarnings("unchecked")
    void facetWithMatch_filteredFacets() {
        Map result = service.facetWithMatch("Books");

        List<Map> totalCount = (List<Map>) result.get("totalCount");
        assertThat(((Number) totalCount.getFirst().get("total")).intValue()).isEqualTo(2);

        List<Map> data = (List<Map>) result.get("data");
        assertThat(data).hasSize(2);
    }

    @Test
    @Order(6)
    @SuppressWarnings("unchecked")
    void facetCombined_realWorldSearch() {
        Map result = service.facetCombinedSearch("Electronics", true, 0, 10);

        // Only inStock Electronics: Mouse(29.99) + Keyboard(89.99) = 2
        List<Map> totalCount = (List<Map>) result.get("totalCount");
        assertThat(((Number) totalCount.getFirst().get("total")).intValue()).isEqualTo(2);

        List<Map> data = (List<Map>) result.get("data");
        assertThat(data).hasSize(2);

        List<Map> stats = (List<Map>) result.get("stats");
        assertThat(stats).hasSize(1);
        assertThat(stats.getFirst()).containsKeys("count", "avgPrice", "avgRating");
    }
}
