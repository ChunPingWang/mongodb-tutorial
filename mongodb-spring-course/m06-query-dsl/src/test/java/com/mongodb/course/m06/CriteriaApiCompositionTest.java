package com.mongodb.course.m06;

import com.mongodb.course.m06.insurance.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LAB-02: Criteria 組合查詢 — Insurance domain
 */
@SpringBootTest
@Import(SharedContainersConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CriteriaApiCompositionTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private InsurancePolicyQueryService queryService;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(InsurancePolicyDocument.class);

        LocalDate now = LocalDate.of(2025, 6, 1);

        InsurancePolicyDocument p1 = new InsurancePolicyDocument("POL-001", "Alice Wang", PolicyType.TERM_LIFE,
                new BigDecimal("1200"), new BigDecimal("500000"),
                now.minusYears(1), now.plusMonths(3));
        p1.setStatus(PolicyStatus.ACTIVE);

        InsurancePolicyDocument p2 = new InsurancePolicyDocument("POL-002", "Bob Chen", PolicyType.HEALTH,
                new BigDecimal("800"), new BigDecimal("200000"),
                now.minusYears(1), now.plusYears(1));
        p2.setStatus(PolicyStatus.ACTIVE);

        InsurancePolicyDocument p3 = new InsurancePolicyDocument("POL-003", "Charlie Lin", PolicyType.AUTO,
                new BigDecimal("600"), new BigDecimal("100000"),
                now.minusYears(2), now.minusMonths(1));
        p3.setStatus(PolicyStatus.EXPIRED);

        InsurancePolicyDocument p4 = new InsurancePolicyDocument("POL-004", "David Wu", PolicyType.WHOLE_LIFE,
                new BigDecimal("2500"), new BigDecimal("1000000"),
                now.minusYears(3), now.plusYears(5));
        p4.setStatus(PolicyStatus.ACTIVE);

        InsurancePolicyDocument p5 = new InsurancePolicyDocument("POL-005", "Eve Huang", PolicyType.HEALTH,
                new BigDecimal("900"), new BigDecimal("300000"),
                now.minusMonths(6), now.plusMonths(6));
        p5.setStatus(PolicyStatus.ACTIVE);

        InsurancePolicyDocument p6 = new InsurancePolicyDocument("POL-006", "Frank Tsai", PolicyType.TERM_LIFE,
                new BigDecimal("1500"), new BigDecimal("700000"),
                now.minusYears(1), now.plusYears(2));
        p6.setStatus(PolicyStatus.CANCELLED);

        mongoTemplate.insertAll(List.of(p1, p2, p3, p4, p5, p6));
    }

    @Test
    @Order(1)
    void orOperator_composesWithOR() {
        // TERM_LIFE or EXPIRED status
        List<InsurancePolicyDocument> results = mongoTemplate.find(
                new org.springframework.data.mongodb.core.query.Query(
                        new org.springframework.data.mongodb.core.query.Criteria().orOperator(
                                org.springframework.data.mongodb.core.query.Criteria.where("policyType").is(PolicyType.TERM_LIFE),
                                org.springframework.data.mongodb.core.query.Criteria.where("status").is(PolicyStatus.EXPIRED)
                        )
                ), InsurancePolicyDocument.class);
        // POL-001 (TERM_LIFE+ACTIVE), POL-003 (AUTO+EXPIRED), POL-006 (TERM_LIFE+CANCELLED)
        assertThat(results).hasSize(3);
    }

    @Test
    @Order(2)
    void andOperator_composesMultipleCriteria() {
        List<InsurancePolicyDocument> results = queryService.findActiveHighCoverage(new BigDecimal("250000"));
        // ACTIVE + coverage >= 250000: POL-001(500k), POL-002(200k NO), POL-004(1M), POL-005(300k)
        assertThat(results).hasSize(3);
        assertThat(results).allMatch(p -> p.getStatus() == PolicyStatus.ACTIVE);
        assertThat(results).allMatch(p -> p.getCoverageAmount().compareTo(new BigDecimal("250000")) >= 0);
    }

    @Test
    @Order(3)
    void in_matchesPolicyTypesFromList() {
        List<InsurancePolicyDocument> results = queryService.findByPolicyTypes(
                List.of(PolicyType.TERM_LIFE, PolicyType.WHOLE_LIFE));
        assertThat(results).hasSize(3); // POL-001, POL-004, POL-006
    }

    @Test
    @Order(4)
    void nin_excludesPolicyTypes() {
        List<InsurancePolicyDocument> results = queryService.findExcludingTypes(
                List.of(PolicyType.AUTO, PolicyType.HEALTH));
        assertThat(results).hasSize(3); // TERM_LIFE x2 + WHOLE_LIFE x1
        assertThat(results).noneMatch(p ->
                p.getPolicyType() == PolicyType.AUTO || p.getPolicyType() == PolicyType.HEALTH);
    }

    @Test
    @Order(5)
    void dateRange_findExpiringSoon() {
        // Expiring before 2025-12-31, only ACTIVE
        List<InsurancePolicyDocument> results = queryService.findExpiringSoon(LocalDate.of(2025, 12, 31));
        // POL-001 (expires 2025-09-01), POL-005 (expires 2025-12-01) — both ACTIVE and before 2025-12-31
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(p -> p.getStatus() == PolicyStatus.ACTIVE);
    }

    @Test
    @Order(6)
    void ne_excludesCancelledPolicies() {
        // Use findByMultipleConditions with non-null status
        List<InsurancePolicyDocument> results = queryService.findByMultipleConditions(null, PolicyStatus.ACTIVE, null);
        assertThat(results).hasSize(4);
        assertThat(results).noneMatch(p -> p.getStatus() == PolicyStatus.CANCELLED);
    }

    @Test
    @Order(7)
    void dynamicQuery_buildsConditionally() {
        // Only type + minPremium, no status
        List<InsurancePolicyDocument> results = queryService.findByMultipleConditions(
                PolicyType.HEALTH, null, new BigDecimal("850"));
        // POL-005 (HEALTH, premium 900) — POL-002 (HEALTH, premium 800) excluded by min
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getPolicyNumber()).isEqualTo("POL-005");
    }

    @Test
    @Order(8)
    void nestedAndOr_combinedCriteria() {
        // (TERM_LIFE OR WHOLE_LIFE) AND ACTIVE
        var results = mongoTemplate.find(
                new org.springframework.data.mongodb.core.query.Query(
                        new org.springframework.data.mongodb.core.query.Criteria().andOperator(
                                new org.springframework.data.mongodb.core.query.Criteria().orOperator(
                                        org.springframework.data.mongodb.core.query.Criteria.where("policyType").is(PolicyType.TERM_LIFE),
                                        org.springframework.data.mongodb.core.query.Criteria.where("policyType").is(PolicyType.WHOLE_LIFE)
                                ),
                                org.springframework.data.mongodb.core.query.Criteria.where("status").is(PolicyStatus.ACTIVE)
                        )
                ), InsurancePolicyDocument.class);
        // POL-001 (TERM_LIFE, ACTIVE), POL-004 (WHOLE_LIFE, ACTIVE) — POL-006 (TERM_LIFE, CANCELLED) excluded
        assertThat(results).hasSize(2);
    }

    @Test
    @Order(9)
    void findNotExpired_gtOnDate() {
        List<InsurancePolicyDocument> results = queryService.findNotExpired(LocalDate.of(2025, 6, 1));
        // Expiration > 2025-06-01: POL-002(2026-06-01), POL-004(2030-06-01), POL-005(2025-12-01), POL-006(2027-06-01)
        // POL-001 expires 2025-09-01 > 2025-06-01 → included
        assertThat(results).hasSize(5); // all except POL-003 (expired 2025-05-01)
    }

    @Test
    @Order(10)
    void premiumRange_worksWithDecimal128() {
        List<InsurancePolicyDocument> results = queryService.findByPremiumRange(
                new BigDecimal("700"), new BigDecimal("1300"));
        // 800, 900, 1200 → 3 results
        assertThat(results).hasSize(3);
        assertThat(results).allMatch(p ->
                p.getPremium().compareTo(new BigDecimal("700")) >= 0 &&
                        p.getPremium().compareTo(new BigDecimal("1300")) <= 0);
    }
}
