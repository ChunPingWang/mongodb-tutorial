package com.mongodb.course.m04;

import com.mongodb.course.m04.java23.*;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M04-LAB-02: Java 23 features with MongoDB.
 * - Record classes as documents
 * - Sealed interfaces for polymorphism
 * - Pattern matching with switch expressions
 */
@SpringBootTest
@Import(SharedContainersConfig.class)
class Java23FeaturesTest {

    @Autowired
    private FinancialProductService productService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.dropCollection("financial_products");
    }

    @Test
    @DisplayName("Store and retrieve Record class via MongoTemplate")
    void recordClassWithMongoTemplate() {
        var savings = new SavingsAccount("sa-001", "My Savings", new BigDecimal("50000"), new BigDecimal("2.5"));

        SavingsAccount saved = productService.save(savings);

        // Read back as the same record type
        var found = productService.findAllSavings();
        assertThat(found).hasSize(1);
        assertThat(found.getFirst().name()).isEqualTo("My Savings");
        assertThat(found.getFirst().interestRate()).isEqualByComparingTo(new BigDecimal("2.5"));
    }

    @Test
    @DisplayName("Sealed interface with pattern matching switch")
    void sealedInterfacePatternMatching() {
        FinancialProduct savings = new SavingsAccount("sa-002", "High Yield", new BigDecimal("100000"), new BigDecimal("3.5"));
        FinancialProduct fixedDeposit = new FixedDeposit("fd-001", "1-Year FD", new BigDecimal("200000"), 12);
        FinancialProduct insurance = new InsurancePolicy("ip-001", "Life Shield", new BigDecimal("500000"), "term-life");

        // Pattern matching with switch — exhaustive because of sealed interface
        assertThat(productService.describe(savings)).contains("3.5%");
        assertThat(productService.describe(fixedDeposit)).contains("12 months");
        assertThat(productService.describe(insurance)).contains("term-life");
    }

    @Test
    @DisplayName("Polymorphic products in same collection with _class discriminator")
    void polymorphicProductsInSameCollection() {
        productService.save(new SavingsAccount("sa-003", "Basic Savings", new BigDecimal("10000"), new BigDecimal("1.5")));
        productService.save(new FixedDeposit("fd-002", "6-Month FD", new BigDecimal("50000"), 6));
        productService.save(new InsurancePolicy("ip-002", "Health Plan", new BigDecimal("100000"), "health"));

        // Each type stored in same collection with _class discriminator
        assertThat(productService.findAllSavings()).hasSize(1);
        assertThat(productService.findAllFixedDeposits()).hasSize(1);
        assertThat(productService.findAllInsurancePolicies()).hasSize(1);

        // Verify _class field exists in raw documents
        Document raw = mongoTemplate.getCollection("financial_products")
                .find(new Document("_id", "sa-003")).first();
        assertThat(raw).isNotNull();
        assertThat(raw.getString("_class")).contains("SavingsAccount");
    }

    @Test
    @DisplayName("Record as embedded value object")
    void recordAsEmbeddedValueObject() {
        var savings = new SavingsAccount("sa-004", "Premium Savings", new BigDecimal("80000"), new BigDecimal("2.8"));
        productService.save(savings);

        // Verify the record fields are properly stored
        Document raw = mongoTemplate.getCollection("financial_products")
                .find(new Document("_id", "sa-004")).first();
        assertThat(raw).isNotNull();
        assertThat(raw.getString("name")).isEqualTo("Premium Savings");
        assertThat(raw.get("value")).isNotNull();
        assertThat(raw.get("interestRate")).isNotNull();
    }
}
