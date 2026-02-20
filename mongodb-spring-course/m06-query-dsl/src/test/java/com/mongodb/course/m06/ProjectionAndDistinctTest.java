package com.mongodb.course.m06;

import com.mongodb.course.m06.banking.*;
import com.mongodb.course.m06.ecommerce.Product;
import com.mongodb.course.m06.ecommerce.ProductQueryService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LAB-03: Projection 與 Distinct 查詢
 */
@SpringBootTest
@Import(SharedContainersConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProjectionAndDistinctTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private BankAccountQueryService bankQueryService;

    @Autowired
    private ProductQueryService productQueryService;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(BankAccount.class);
        mongoTemplate.dropCollection(Product.class);

        // Banking data
        BankAccount a1 = new BankAccount("ACC-001", "Alice Wang", AccountType.SAVINGS, new BigDecimal("50000"));
        BankAccount a2 = new BankAccount("ACC-002", "Bob Chen", AccountType.CHECKING, new BigDecimal("15000"));
        BankAccount a3 = new BankAccount("ACC-003", "Alice Wang", AccountType.CHECKING, new BigDecimal("30000"));
        mongoTemplate.insertAll(List.of(a1, a2, a3));

        // Product data
        Product p1 = new Product("SKU-001", "Wireless Mouse", "Electronics", new BigDecimal("599"), true);
        Product p2 = new Product("SKU-002", "Mechanical Keyboard", "Electronics", new BigDecimal("2499"), true);
        Product p3 = new Product("SKU-003", "Java Programming Book", "Books", new BigDecimal("890"), true);
        Product p4 = new Product("SKU-004", "USB-C Cable", "Electronics", new BigDecimal("199"), false);
        mongoTemplate.insertAll(List.of(p1, p2, p3, p4));
    }

    @Test
    @Order(1)
    void projection_includesOnlySpecifiedFields() {
        List<BankAccount> results = bankQueryService.findAccountNumbersOnly();
        assertThat(results).hasSize(3);
        assertThat(results).allMatch(a -> a.getAccountNumber() != null);
        // Other fields should be null/default due to projection
        assertThat(results).allMatch(a -> a.getHolderName() == null);
    }

    @Test
    @Order(2)
    void projection_excludesIdField() {
        List<BankAccount> results = bankQueryService.findAccountNumbersOnly();
        assertThat(results).allMatch(a -> a.getId() == null);
    }

    @Test
    @Order(3)
    void findDistinct_holderNames() {
        List<String> names = bankQueryService.findDistinctHolderNames();
        // Alice Wang appears twice but distinct should return once
        assertThat(names).hasSize(2);
        assertThat(names).containsExactlyInAnyOrder("Alice Wang", "Bob Chen");
    }

    @Test
    @Order(4)
    void findDistinct_categories() {
        List<String> categories = productQueryService.findDistinctCategories();
        assertThat(categories).hasSize(2);
        assertThat(categories).containsExactlyInAnyOrder("Electronics", "Books");
    }

    @Test
    @Order(5)
    void projection_withCriteriaFilter() {
        Query query = new Query(Criteria.where("type").is(AccountType.CHECKING));
        query.fields().include("accountNumber", "holderName");
        List<BankAccount> results = mongoTemplate.find(query, BankAccount.class);
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(a -> a.getAccountNumber() != null && a.getHolderName() != null);
    }

    @Test
    @Order(6)
    void findWithLimit_capsResults() {
        Query query = new Query().limit(2);
        List<BankAccount> results = mongoTemplate.find(query, BankAccount.class);
        assertThat(results).hasSize(2);
    }
}
