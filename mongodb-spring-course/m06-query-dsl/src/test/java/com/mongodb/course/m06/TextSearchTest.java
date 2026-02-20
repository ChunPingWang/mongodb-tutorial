package com.mongodb.course.m06;

import com.mongodb.course.m06.ecommerce.Product;
import com.mongodb.course.m06.ecommerce.ProductQueryService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LAB-04: 全文檢索 — Product domain
 */
@SpringBootTest
@Import(SharedContainersConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TextSearchTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ProductQueryService queryService;

    @BeforeAll
    static void createIndex(@Autowired MongoTemplate mongoTemplate) {
        mongoTemplate.dropCollection(Product.class);
        // Programmatically create text index
        TextIndexDefinition textIndex = new TextIndexDefinition.TextIndexDefinitionBuilder()
                .onField("name", 3F)
                .onField("description")
                .build();
        mongoTemplate.indexOps(Product.class).ensureIndex(textIndex);
    }

    @BeforeEach
    void setUp() {
        mongoTemplate.remove(new org.springframework.data.mongodb.core.query.Query(), Product.class);

        Product p1 = new Product("SKU-001", "Wireless Bluetooth Mouse", "Electronics", new BigDecimal("599"), true);
        p1.setDescription("A compact wireless mouse with Bluetooth connectivity and ergonomic design");
        p1.setRating(4.5);

        Product p2 = new Product("SKU-002", "Mechanical Gaming Keyboard", "Electronics", new BigDecimal("2499"), true);
        p2.setDescription("RGB mechanical keyboard with Cherry MX switches for gaming enthusiasts");
        p2.setRating(4.8);

        Product p3 = new Product("SKU-003", "Java Programming Guide", "Books", new BigDecimal("890"), true);
        p3.setDescription("Comprehensive guide to Java programming covering Spring Boot and MongoDB");
        p3.setRating(4.2);

        Product p4 = new Product("SKU-004", "USB-C Hub Adapter", "Electronics", new BigDecimal("799"), true);
        p4.setDescription("Multi-port USB-C hub with HDMI, USB 3.0, and SD card reader");
        p4.setRating(4.0);

        Product p5 = new Product("SKU-005", "Wireless Noise Cancelling Headphones", "Electronics", new BigDecimal("3999"), false);
        p5.setDescription("Premium wireless headphones with active noise cancelling technology");
        p5.setRating(4.7);

        Product p6 = new Product("SKU-006", "MongoDB in Action", "Books", new BigDecimal("1200"), true);
        p6.setDescription("Practical guide to MongoDB database design and query optimization");
        p6.setRating(4.3);

        mongoTemplate.insertAll(List.of(p1, p2, p3, p4, p5, p6));
    }

    @Test
    @Order(1)
    void textSearch_matchesSingleTermInName() {
        List<Product> results = queryService.textSearch("wireless");
        // "Wireless Bluetooth Mouse", "Wireless Noise Cancelling Headphones" in name;
        // p1 also has "wireless" in description
        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
        assertThat(results).anyMatch(p -> p.getName().contains("Wireless"));
    }

    @Test
    @Order(2)
    void textSearch_matchesTermInDescription() {
        List<Product> results = queryService.textSearch("ergonomic");
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getSku()).isEqualTo("SKU-001");
    }

    @Test
    @Order(3)
    void textSearch_matchingAnyReturnsUnion() {
        List<Product> results = queryService.textSearch("keyboard", "mouse");
        // Mouse (name+desc), Keyboard (name+desc)
        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @Order(4)
    void textSearch_matchingPhraseRequiresExact() {
        List<Product> results = queryService.textSearchPhrase("noise cancelling");
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getSku()).isEqualTo("SKU-005");
    }

    @Test
    @Order(5)
    void textSearch_notMatchingExcludesTerm() {
        // Search "wireless" but exclude "mouse"
        List<Product> results = queryService.textSearchExcluding("wireless", "mouse");
        // Should exclude SKU-001 (wireless mouse), keep SKU-005 (wireless headphones)
        assertThat(results).allMatch(p -> !p.getSku().equals("SKU-001"));
        assertThat(results).anyMatch(p -> p.getSku().equals("SKU-005"));
    }

    @Test
    @Order(6)
    void textSearch_sortedByScore() {
        // "MongoDB" appears in name (weight 3) of SKU-006 and description of SKU-003
        List<Product> results = queryService.textSearchWithScoreSort("MongoDB");
        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
        // SKU-006 "MongoDB in Action" (name, weight 3) should rank higher
        assertThat(results.getFirst().getSku()).isEqualTo("SKU-006");
    }

    @Test
    @Order(7)
    void textSearch_combinedWithCategoryFilter() {
        List<Product> results = queryService.textSearchInCategory("guide", "Books");
        // "Java Programming Guide" (name) and "MongoDB in Action" (description has "guide")
        assertThat(results).hasSizeGreaterThanOrEqualTo(1);
        assertThat(results).allMatch(p -> p.getCategory().equals("Books"));
    }

    @Test
    @Order(8)
    void textSearch_noResultsReturnsEmpty() {
        List<Product> results = queryService.textSearch("nonexistentxyz");
        assertThat(results).isEmpty();
    }
}
