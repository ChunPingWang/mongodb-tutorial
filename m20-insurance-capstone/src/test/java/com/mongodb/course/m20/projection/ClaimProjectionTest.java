package com.mongodb.course.m20.projection;

import com.mongodb.course.m20.SharedContainersConfig;
import com.mongodb.course.m20.claim.service.ClaimCommandService;
import com.mongodb.course.m20.policy.AutoPolicy;
import com.mongodb.course.m20.policy.HealthPolicy;
import com.mongodb.course.m20.policy.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class ClaimProjectionTest {

    @Autowired private ClaimCommandService claimCommandService;
    @Autowired private PolicyService policyService;
    @Autowired private ClaimQueryService claimQueryService;
    @Autowired private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.remove(new Query(), "m20_claim_events");
        mongoTemplate.remove(new Query(), "m20_snapshots");
        mongoTemplate.remove(new Query(), "m20_claim_dashboard");
        mongoTemplate.remove(new Query(), "m20_claim_statistics");
        mongoTemplate.remove(new Query(), "m20_settlement_saga_logs");
        mongoTemplate.remove(new Query(), "m20_claim_notifications");
        if (mongoTemplate.collectionExists("m20_policies")) {
            mongoTemplate.dropCollection("m20_policies");
        }
        new com.mongodb.course.m20.config.SchemaValidationConfig(mongoTemplate).init();
    }

    @Test
    void dashboardReflectsClaimProgress() {
        var policy = new AutoPolicy("pol-p1", "POL-P01", "Wang",
                new BigDecimal("10000"), new BigDecimal("500000"), "Sedan");
        policyService.save(policy);

        claimCommandService.fileClaim("CLM-P01", "pol-p1", "Wang", "AUTO",
                new BigDecimal("40000"), "Accident");
        claimCommandService.investigate("CLM-P01", "Inspector", "OK", "LOW");
        claimCommandService.assess("CLM-P01", new BigDecimal("35000"), "Fair");

        var dashboard = claimQueryService.findDashboardByClaimId("CLM-P01");
        assertThat(dashboard).isPresent();
        assertThat(dashboard.get().status()).isEqualTo("ASSESSED");
        assertThat(dashboard.get().timeline()).hasSize(3);
        assertThat(dashboard.get().assessedAmount()).isEqualByComparingTo(new BigDecimal("35000"));
    }

    @Test
    void statisticsAggregateByCategory() {
        var autoPolicy = new AutoPolicy("pol-p2", "POL-P02", "Li",
                new BigDecimal("10000"), new BigDecimal("500000"), "Sedan");
        policyService.save(autoPolicy);

        var healthPolicy = new HealthPolicy("pol-p3", "POL-P03", "Zhang",
                new BigDecimal("8000"), new BigDecimal("300000"), "STANDARD");
        policyService.save(healthPolicy);

        claimCommandService.fileClaim("CLM-P02", "pol-p2", "Li", "AUTO",
                new BigDecimal("30000"), "Claim 1");
        claimCommandService.fileClaim("CLM-P03", "pol-p2", "Li", "AUTO",
                new BigDecimal("20000"), "Claim 2");
        claimCommandService.fileClaim("CLM-P04", "pol-p3", "Zhang", "HEALTH",
                new BigDecimal("50000"), "Health claim");

        var autoStats = claimQueryService.findStatisticsByCategory("AUTO");
        assertThat(autoStats).isPresent();
        assertThat(autoStats.get().totalClaims()).isEqualTo(2);
        assertThat(autoStats.get().totalClaimedAmount()).isEqualByComparingTo(new BigDecimal("50000"));

        var healthStats = claimQueryService.findStatisticsByCategory("HEALTH");
        assertThat(healthStats).isPresent();
        assertThat(healthStats.get().totalClaims()).isEqualTo(1);
    }
}
