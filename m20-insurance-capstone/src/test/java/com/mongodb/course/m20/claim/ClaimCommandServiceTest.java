package com.mongodb.course.m20.claim;

import com.mongodb.course.m20.SharedContainersConfig;
import com.mongodb.course.m20.claim.model.ClaimStatus;
import com.mongodb.course.m20.claim.service.ClaimCommandService;
import com.mongodb.course.m20.infrastructure.EventStore;
import com.mongodb.course.m20.policy.AutoPolicy;
import com.mongodb.course.m20.policy.PolicyService;
import com.mongodb.course.m20.projection.ClaimQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(SharedContainersConfig.class)
class ClaimCommandServiceTest {

    @Autowired private ClaimCommandService claimCommandService;
    @Autowired private PolicyService policyService;
    @Autowired private EventStore eventStore;
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
        // Drop and recreate policies collection to reset schema validation
        if (mongoTemplate.collectionExists("m20_policies")) {
            mongoTemplate.dropCollection("m20_policies");
        }
        new com.mongodb.course.m20.config.SchemaValidationConfig(mongoTemplate).init();
    }

    @Test
    void fileAndInvestigateClaim() {
        var policy = new AutoPolicy("pol-1", "POL-001", "Wang",
                new BigDecimal("10000"), new BigDecimal("500000"), "Sedan");
        policyService.save(policy);

        claimCommandService.fileClaim("CLM-001", "pol-1", "Wang", "AUTO",
                new BigDecimal("50000"), "Car accident");
        claimCommandService.investigate("CLM-001", "Inspector A", "Minor damage", "LOW");

        var claim = claimCommandService.loadClaim("CLM-001");
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.UNDER_INVESTIGATION);
        assertThat(claim.getFraudRisk()).isEqualTo("LOW");
        assertThat(eventStore.countEvents("CLM-001", "m20_claim_events")).isEqualTo(2);
    }

    @Test
    void assessmentEnforcesAmountLimit() {
        var policy = new AutoPolicy("pol-2", "POL-002", "Li",
                new BigDecimal("10000"), new BigDecimal("300000"), "SUV");
        policyService.save(policy);

        claimCommandService.fileClaim("CLM-002", "pol-2", "Li", "AUTO",
                new BigDecimal("80000"), "Fender bender");
        claimCommandService.investigate("CLM-002", "Inspector B", "Moderate damage", "LOW");

        assertThatThrownBy(() ->
                claimCommandService.assess("CLM-002", new BigDecimal("90000"), "Over-assessed"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exceeds claimed amount");
    }

    @Test
    void fullClaimLifecycle() {
        var policy = new AutoPolicy("pol-3", "POL-003", "Zhang",
                new BigDecimal("15000"), new BigDecimal("1000000"), "Truck");
        policyService.save(policy);

        claimCommandService.fileClaim("CLM-003", "pol-3", "Zhang", "AUTO",
                new BigDecimal("100000"), "Highway collision");
        claimCommandService.investigate("CLM-003", "Inspector C", "Verified", "LOW");
        claimCommandService.assess("CLM-003", new BigDecimal("80000"), "Fair assessment");
        claimCommandService.approve("CLM-003", new BigDecimal("80000"));
        claimCommandService.pay("CLM-003", new BigDecimal("80000"), "PAY-001");

        var claim = claimCommandService.loadClaim("CLM-003");
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.PAID);
        assertThat(eventStore.countEvents("CLM-003", "m20_claim_events")).isEqualTo(5);

        var dashboard = claimQueryService.findDashboardByClaimId("CLM-003");
        assertThat(dashboard).isPresent();
        assertThat(dashboard.get().status()).isEqualTo("PAID");

        var stats = claimQueryService.findStatisticsByCategory("AUTO");
        assertThat(stats).isPresent();
        assertThat(stats.get().paidCount()).isEqualTo(1);
    }
}
