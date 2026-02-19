package com.mongodb.course.m02;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.mongodb.course.m02.cassandra.CassandraProductService;
import com.mongodb.course.m02.mongo.ProductDocument;
import com.mongodb.course.m02.mongo.ProductMongoRepository;
import com.mongodb.course.m02.redis.RedisProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M02-LAB-01: Compare three NoSQL database types side by side.
 * - MongoDB (Document Store): flexible queries, schema-free
 * - Redis (Key-Value Store): ultra-fast, exact key lookup only
 * - Cassandra (Wide-Column Store): partition-key driven queries
 */
@SpringBootTest
@Import(SharedContainersConfig.class)
class ThreeNoSqlTypesComparisonTest {

    @Autowired
    private ProductMongoRepository mongoRepository;

    @Autowired
    private RedisProductService redisService;

    @Autowired
    private CassandraProductService cassandraService;

    @Autowired
    private CqlSession cqlSession;

    @BeforeEach
    void cleanUp() {
        mongoRepository.deleteAll();
        cqlSession.execute("TRUNCATE products");
    }

    @Test
    @DisplayName("MongoDB: store and retrieve with flexible queries")
    void mongoDbDocumentStore() {
        var laptop = new ProductDocument("Laptop Pro", "electronics", new BigDecimal("35000"));
        var mouse = new ProductDocument("Wireless Mouse", "electronics", new BigDecimal("500"));
        var book = new ProductDocument("Spring in Action", "books", new BigDecimal("1200"));

        mongoRepository.saveAll(List.of(laptop, mouse, book));

        // Query by category (ad-hoc query — no special schema design needed)
        List<ProductDocument> electronics = mongoRepository.findByCategory("electronics");
        assertThat(electronics).hasSize(2);

        // Query by name containing keyword
        List<ProductDocument> laptops = mongoRepository.findByNameContaining("Laptop");
        assertThat(laptops).hasSize(1);
        assertThat(laptops.getFirst().getName()).isEqualTo("Laptop Pro");
    }

    @Test
    @DisplayName("Redis: store and retrieve by exact key only")
    void redisKeyValueStore() {
        redisService.save("product:laptop", "Laptop Pro|electronics|35000");
        redisService.save("product:mouse", "Wireless Mouse|electronics|500");

        // Exact key lookup — the ONLY query pattern
        String laptop = redisService.findByKey("product:laptop");
        assertThat(laptop).isEqualTo("Laptop Pro|electronics|35000");

        // Cannot query by category or name — must know the exact key
        String unknown = redisService.findByKey("product:unknown");
        assertThat(unknown).isNull();
    }

    @Test
    @DisplayName("Cassandra: store and retrieve by partition key")
    void cassandraWideColumnStore() {
        cassandraService.save("laptop-1", "Laptop Pro", "electronics", new BigDecimal("35000"));
        cassandraService.save("mouse-1", "Wireless Mouse", "electronics", new BigDecimal("500"));
        cassandraService.save("book-1", "Spring in Action", "books", new BigDecimal("1200"));

        // Query by partition key (category) — Cassandra's strength
        List<Row> electronics = cassandraService.findByCategory("electronics");
        assertThat(electronics).hasSize(2);

        // Query by partition key + clustering key
        Row laptop = cassandraService.findByCategoryAndId("electronics", "laptop-1");
        assertThat(laptop).isNotNull();
        assertThat(laptop.getString("name")).isEqualTo("Laptop Pro");
    }

    @Test
    @DisplayName("Query pattern differences across three NoSQL types")
    void queryPatternComparison() {
        // Setup: same product in all three stores
        var product = new ProductDocument("Laptop Pro", "electronics", new BigDecimal("35000"));
        ProductDocument saved = mongoRepository.save(product);

        redisService.save("product:" + saved.getId(), saved.getName());

        cassandraService.save(saved.getId(), saved.getName(), saved.getCategory(), saved.getPrice());

        // MongoDB: ad-hoc query by any field
        assertThat(mongoRepository.findByCategory("electronics")).isNotEmpty();
        assertThat(mongoRepository.findByNameContaining("Laptop")).isNotEmpty();

        // Redis: exact key lookup only
        assertThat(redisService.findByKey("product:" + saved.getId())).isNotNull();
        // Cannot: redisService.findByCategory("electronics") — no such capability

        // Cassandra: partition key query
        assertThat(cassandraService.findByCategory("electronics")).isNotEmpty();
        // Must include partition key: findByCategoryAndId works, findById alone doesn't
        assertThat(cassandraService.findByCategoryAndId("electronics", saved.getId())).isNotNull();
    }

    @Test
    @DisplayName("Schema flexibility comparison across three NoSQL types")
    void schemaFlexibilityComparison() {
        // MongoDB: store documents with different shapes (no ALTER TABLE needed)
        var basicProduct = new ProductDocument("Mouse", "electronics", new BigDecimal("500"));
        var richProduct = new ProductDocument("Laptop Pro", "electronics", new BigDecimal("35000"),
                Map.of("cpu", "M3 Pro", "ram", "18GB", "storage", "512GB SSD"));

        mongoRepository.saveAll(List.of(basicProduct, richProduct));

        ProductDocument foundBasic = mongoRepository.findById(basicProduct.getId()).orElseThrow();
        ProductDocument foundRich = mongoRepository.findById(richProduct.getId()).orElseThrow();

        // Different documents can have different fields
        assertThat(foundBasic.getSpecifications()).isNull();
        assertThat(foundRich.getSpecifications()).containsKeys("cpu", "ram", "storage");

        // Redis: any string value — no schema at all
        redisService.save("simple", "just a string");
        redisService.save("json", "{\"complex\":true,\"nested\":{\"key\":\"value\"}}");
        assertThat(redisService.findByKey("simple")).isEqualTo("just a string");
        assertThat(redisService.findByKey("json")).contains("complex");

        // Cassandra: rigid column schema — adding new columns requires ALTER TABLE
        cassandraService.save("p1", "Mouse", "electronics", new BigDecimal("500"));
        Row row = cassandraService.findByCategoryAndId("electronics", "p1");
        assertThat(row.getString("name")).isEqualTo("Mouse");
        // Cannot add arbitrary fields without ALTER TABLE
    }
}
