package com.mongodb.course.m07;

import com.mongodb.course.m07.ecommerce.OrderItem;
import com.mongodb.course.m07.ecommerce.OrderStatus;
import com.mongodb.course.m07.ecommerce.Product;
import com.mongodb.course.m07.service.LookupAggregationService;
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
 * LAB-04: $lookup — 跨集合 Join
 */
@SpringBootTest
@Import(SharedContainersConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LookupAggregationTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private LookupAggregationService service;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(Product.class);
        mongoTemplate.dropCollection(com.mongodb.course.m07.ecommerce.Order.class);

        // Products
        Product p1 = new Product("SKU-001", "Wireless Mouse", "Electronics", new BigDecimal("29.99"), true);
        p1.setRating(4.5);
        Product p2 = new Product("SKU-002", "Keyboard", "Electronics", new BigDecimal("89.99"), true);
        p2.setRating(4.8);
        Product p3 = new Product("SKU-003", "Java Book", "Books", new BigDecimal("45.00"), true);
        p3.setRating(4.2);
        mongoTemplate.insertAll(List.of(p1, p2, p3));

        // Orders with items referencing products by sku
        var o1 = new com.mongodb.course.m07.ecommerce.Order("ORD-001", "Alice", new BigDecimal("150"), OrderStatus.DELIVERED, LocalDate.of(2025, 1, 15));
        o1.setItems(List.of(
                new OrderItem("SKU-001", "Wireless Mouse", 2, new BigDecimal("29.99")),
                new OrderItem("SKU-002", "Keyboard", 1, new BigDecimal("89.99"))
        ));
        var o2 = new com.mongodb.course.m07.ecommerce.Order("ORD-002", "Bob", new BigDecimal("45"), OrderStatus.SHIPPED, LocalDate.of(2025, 2, 10));
        o2.setItems(List.of(
                new OrderItem("SKU-003", "Java Book", 1, new BigDecimal("45.00"))
        ));
        var o3 = new com.mongodb.course.m07.ecommerce.Order("ORD-003", "Alice", new BigDecimal("90"), OrderStatus.CONFIRMED, LocalDate.of(2025, 3, 5));
        o3.setItems(List.of(
                new OrderItem("SKU-002", "Keyboard", 1, new BigDecimal("89.99"))
        ));
        mongoTemplate.insertAll(List.of(o1, o2, o3));
    }

    @Test
    @Order(1)
    @SuppressWarnings("unchecked")
    void basicLookup_ordersJoinProducts() {
        List<Map> results = service.ordersWithProductDetails();
        assertThat(results).isNotEmpty();

        // ORD-001 has 2 items, each unwound → 2 rows for ORD-001
        long ord1Count = results.stream()
                .filter(r -> "ORD-001".equals(r.get("orderNumber"))).count();
        assertThat(ord1Count).isEqualTo(2);

        // Each should have productDetails array
        Map first = results.stream()
                .filter(r -> "ORD-001".equals(r.get("orderNumber"))).findFirst().orElseThrow();
        List<Map> productDetails = (List<Map>) first.get("productDetails");
        assertThat(productDetails).isNotEmpty();
    }

    @Test
    @Order(2)
    void lookupWithUnwind_flattenJoined() {
        List<Map> results = service.lookupWithUnwind();
        assertThat(results).isNotEmpty();

        // Should have flat results with productName from lookup
        Map first = results.stream()
                .filter(r -> "SKU-001".equals(r.get("sku"))).findFirst().orElseThrow();
        assertThat(first.get("productName")).isEqualTo("Wireless Mouse");
        assertThat(first.get("productPrice")).isNotNull();
    }

    @Test
    @Order(3)
    void customerOrderSummary_groupByCustomer() {
        List<Map> results = service.customerOrderSummary();
        assertThat(results).hasSize(2); // Alice, Bob

        Map alice = results.stream()
                .filter(r -> "Alice".equals(r.get("customerName"))).findFirst().orElseThrow();
        assertThat(((Number) alice.get("orderCount")).intValue()).isEqualTo(2);
        BigDecimal aliceTotal = new BigDecimal(alice.get("totalSpent").toString());
        assertThat(aliceTotal).isEqualByComparingTo(new BigDecimal("240")); // 150+90
    }

    @Test
    @Order(4)
    void pipelineLookup_filterWithinJoin() {
        List<Map> results = service.pipelineLookup();
        assertThat(results).isNotEmpty();

        // Verify joined product info is present
        Map first = results.stream()
                .filter(r -> "SKU-001".equals(r.get("sku"))).findFirst().orElseThrow();
        assertThat(first.get("productName")).isEqualTo("Wireless Mouse");
        assertThat(first.get("productCategory")).isEqualTo("Electronics");
    }

    @Test
    @Order(5)
    void lookupAddFields_productCount() {
        List<Map> results = service.ordersWithProductCount();
        assertThat(results).isNotEmpty();

        // ORD-001 has 2 items, both matched to products
        Map ord1 = results.stream()
                .filter(r -> "ORD-001".equals(r.get("orderNumber"))).findFirst().orElseThrow();
        assertThat(((Number) ord1.get("totalItems")).intValue()).isEqualTo(2);
    }

    @Test
    @Order(6)
    @SuppressWarnings("unchecked")
    void lookupAndProject_selectedFields() {
        List<Map> results = service.lookupAndProject();
        assertThat(results).hasSize(3); // 3 orders

        Map ord1 = results.stream()
                .filter(r -> "ORD-001".equals(r.get("orderNumber"))).findFirst().orElseThrow();
        assertThat(ord1).containsKey("productNames");
        // ORD-001 items have SKU-001 and SKU-002 — lookup on items.sku matches products
        List<String> productNames = (List<String>) ord1.get("productNames");
        assertThat(productNames).isNotEmpty();
    }
}
