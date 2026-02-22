package com.mongodb.course.m07;

import com.mongodb.course.m07.dto.TagCount;
import com.mongodb.course.m07.ecommerce.OrderItem;
import com.mongodb.course.m07.ecommerce.OrderStatus;
import com.mongodb.course.m07.ecommerce.Product;
import com.mongodb.course.m07.service.UnwindAggregationService;
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
 * LAB-03: $unwind — 展開陣列 + 重新分組
 */
@SpringBootTest
@Import(SharedContainersConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UnwindAggregationTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private UnwindAggregationService service;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(Product.class);
        mongoTemplate.dropCollection(com.mongodb.course.m07.ecommerce.Order.class);

        // Products: 5 products (1 with no tags for preserveEmpty test)
        Product p1 = new Product("SKU-001", "Wireless Mouse", "Electronics", new BigDecimal("29.99"), true);
        p1.setRating(4.5);
        p1.setTags(List.of("wireless", "computer"));
        Product p2 = new Product("SKU-002", "Keyboard", "Electronics", new BigDecimal("89.99"), true);
        p2.setRating(4.8);
        p2.setTags(List.of("keyboard", "computer"));
        Product p3 = new Product("SKU-003", "Java Book", "Books", new BigDecimal("45.00"), true);
        p3.setRating(4.2);
        p3.setTags(List.of("java", "programming"));
        Product p4 = new Product("SKU-004", "Spring Book", "Books", new BigDecimal("55.00"), true);
        p4.setRating(4.6);
        p4.setTags(List.of("spring", "java"));
        Product p5 = new Product("SKU-005", "Standing Desk", "Furniture", new BigDecimal("299.99"), true);
        p5.setRating(4.7);
        p5.setTags(List.of()); // empty tags for preserveEmpty test
        mongoTemplate.insertAll(List.of(p1, p2, p3, p4, p5));

        // Orders: 2 orders with multiple items
        var o1 = new com.mongodb.course.m07.ecommerce.Order("ORD-001", "Alice", new BigDecimal("150"), OrderStatus.DELIVERED, LocalDate.of(2025, 1, 15));
        o1.setItems(List.of(
                new OrderItem("SKU-001", "Wireless Mouse", 2, new BigDecimal("29.99")),
                new OrderItem("SKU-002", "Keyboard", 1, new BigDecimal("89.99"))
        ));
        var o2 = new com.mongodb.course.m07.ecommerce.Order("ORD-002", "Bob", new BigDecimal("100"), OrderStatus.SHIPPED, LocalDate.of(2025, 2, 10));
        o2.setItems(List.of(
                new OrderItem("SKU-003", "Java Book", 1, new BigDecimal("45.00")),
                new OrderItem("SKU-004", "Spring Book", 1, new BigDecimal("55.00")),
                new OrderItem("SKU-001", "Wireless Mouse", 1, new BigDecimal("29.99"))
        ));
        mongoTemplate.insertAll(List.of(o1, o2));
    }

    @Test
    @Order(1)
    void unwindTags_countPerTag() {
        List<TagCount> results = service.countByTag();
        assertThat(results).isNotEmpty();

        // "computer" appears in 2 products, "java" appears in 2 products
        TagCount computer = results.stream()
                .filter(r -> "computer".equals(r.tag())).findFirst().orElseThrow();
        assertThat(computer.count()).isEqualTo(2);

        TagCount java = results.stream()
                .filter(r -> "java".equals(r.tag())).findFirst().orElseThrow();
        assertThat(java.count()).isEqualTo(2);
    }

    @Test
    @Order(2)
    void unwindTags_avgPricePerTag() {
        List<Map> results = service.avgPriceByTag();
        assertThat(results).isNotEmpty();

        // "computer" tag: (29.99+89.99)/2 = 59.99
        Map computer = results.stream()
                .filter(r -> "computer".equals(r.get("tag"))).findFirst().orElseThrow();
        double avgPrice = ((Number) computer.get("avgPrice")).doubleValue();
        assertThat(avgPrice).isBetween(59.0, 60.0);
    }

    @Test
    @Order(3)
    void unwindTags_topN() {
        List<TagCount> results = service.topTags(3);
        assertThat(results).hasSize(3);
        // top tags by count: computer(2), java(2), then one of the others
        assertThat(results.getFirst().count()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @Order(4)
    void unwindPreserveEmpty_includesTagless() {
        List<Map> results = service.unwindPreserveEmpty();
        // 4 products with tags generate: 2+2+2+2=8 docs, plus 1 tagless product = 9
        assertThat(results).hasSize(9);

        // Standing Desk should appear with null tag
        long deskCount = results.stream()
                .filter(r -> "Standing Desk".equals(r.get("name")))
                .count();
        assertThat(deskCount).isEqualTo(1);
    }

    @Test
    @Order(5)
    void unwindOrderItems_countPerOrder() {
        List<Map> results = service.itemCountByOrder();
        assertThat(results).hasSize(2);

        Map ord1 = results.stream()
                .filter(r -> "ORD-001".equals(r.get("orderNumber"))).findFirst().orElseThrow();
        assertThat(((Number) ord1.get("itemCount")).intValue()).isEqualTo(2);

        Map ord2 = results.stream()
                .filter(r -> "ORD-002".equals(r.get("orderNumber"))).findFirst().orElseThrow();
        assertThat(((Number) ord2.get("itemCount")).intValue()).isEqualTo(3);
    }

    @Test
    @Order(6)
    @SuppressWarnings("unchecked")
    void unwindAndRegroup_tagsByCategory() {
        List<Map> results = service.tagsByCategory();
        assertThat(results).isNotEmpty();

        // "java" tag should map to Books category
        Map javaTag = results.stream()
                .filter(r -> "java".equals(r.get("tag"))).findFirst().orElseThrow();
        List<String> categories = (List<String>) javaTag.get("categories");
        assertThat(categories).contains("Books");
    }
}
