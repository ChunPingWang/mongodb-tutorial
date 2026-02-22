package com.mongodb.course.m01;

import com.mongodb.course.m01.mongo.InsurancePolicyDocument;
import com.mongodb.course.m01.mongo.InsurancePolicyDocument.RiskAssessment;
import com.mongodb.course.m01.mongo.InsurancePolicyMongoRepository;
import com.mongodb.course.m01.rdb.InsurancePolicyEntity;
import com.mongodb.course.m01.rdb.InsurancePolicyJpaRepository;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M01-LAB-02: Schema Evolution — comparing ALTER TABLE (RDB) vs flexible write (MongoDB).
 * Demonstrates the cost difference of adding fields to existing data models.
 */
@SpringBootTest
@Import(SharedContainersConfig.class)
class SchemaEvolutionTest {

    @Autowired
    private InsurancePolicyJpaRepository jpaRepository;

    @Autowired
    private InsurancePolicyMongoRepository mongoRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        jpaRepository.deleteAll();
        mongoRepository.deleteAll();
    }

    @Test
    @DisplayName("RDB: Adding 'additional clause' field requires ALTER TABLE")
    void rdbSchemaEvolution() {
        // Step 1: Create policy without additional clause (original schema)
        var policy = new InsurancePolicyEntity(
                "POL-001", "Zhang San", "LIFE",
                new BigDecimal("12000"), LocalDate.of(2024, 1, 1), LocalDate.of(2034, 1, 1));
        jpaRepository.save(policy);

        // Step 2: In RDB, adding a new column requires ALTER TABLE
        // ALTER TABLE insurance_policies ADD COLUMN additional_clause VARCHAR(255);
        // Hibernate handles this with ddl-auto=create-drop in tests,
        // but in production this requires a migration script (Flyway/Liquibase)

        // Step 3: Update existing record with new field
        var found = jpaRepository.findByPolicyNumber("POL-001").orElseThrow();
        found.setAdditionalClause("Waiver of Premium on Disability");
        jpaRepository.save(found);

        var updated = jpaRepository.findByPolicyNumber("POL-001").orElseThrow();
        assertThat(updated.getAdditionalClause()).isEqualTo("Waiver of Premium on Disability");
    }

    @Test
    @DisplayName("MongoDB: Adding new fields requires no schema migration")
    void mongoSchemaEvolution() {
        // Step 1: Create policy (original schema — no additional clauses)
        var policy = new InsurancePolicyDocument(
                "POL-001", "Zhang San", "LIFE",
                new BigDecimal("12000"), LocalDate.of(2024, 1, 1), LocalDate.of(2034, 1, 1));
        mongoRepository.save(policy);

        // Step 2: Simply add new fields — no ALTER TABLE, no migration
        var found = mongoRepository.findByPolicyNumber("POL-001").orElseThrow();
        found.addClause("Waiver of Premium on Disability");
        found.addClause("Accidental Death Benefit Rider");
        found.setRiskAssessment(new RiskAssessment("LOW", 0.15, "AutoUnderwriter"));
        mongoRepository.save(found);

        // Step 3: Read back — new fields are there
        var updated = mongoRepository.findByPolicyNumber("POL-001").orElseThrow();
        assertThat(updated.getAdditionalClauses()).containsExactly(
                "Waiver of Premium on Disability",
                "Accidental Death Benefit Rider"
        );
        assertThat(updated.getRiskAssessment().level()).isEqualTo("LOW");
    }

    @Test
    @DisplayName("MongoDB: Old documents without new fields are still readable")
    void mongoBackwardCompatibility() {
        // Insert a "legacy" document directly (simulating an old schema document)
        Document legacyDoc = new Document()
                .append("policyNumber", "POL-LEGACY")
                .append("holderName", "Li Si")
                .append("policyType", "HEALTH")
                .append("premium", new BigDecimal("8000"))
                .append("startDate", LocalDate.of(2023, 6, 1).toString())
                .append("endDate", LocalDate.of(2033, 6, 1).toString());
        // No additionalClauses or riskAssessment fields
        mongoTemplate.insert(legacyDoc, "insurance_policies");

        // Insert a "new schema" document
        var newPolicy = new InsurancePolicyDocument(
                "POL-NEW", "Wang Wu", "AUTO",
                new BigDecimal("5000"), LocalDate.of(2024, 3, 1), LocalDate.of(2025, 3, 1));
        newPolicy.addClause("Glass Coverage");
        newPolicy.setRiskAssessment(new RiskAssessment("MEDIUM", 0.45, "Agent"));
        mongoRepository.save(newPolicy);

        // Both documents coexist — mixed schema versions in the same collection
        assertThat(mongoRepository.findAll()).hasSize(2);

        // New schema document has all fields
        var newFound = mongoRepository.findByPolicyNumber("POL-NEW").orElseThrow();
        assertThat(newFound.getAdditionalClauses()).containsExactly("Glass Coverage");
        assertThat(newFound.getRiskAssessment()).isNotNull();
    }
}
