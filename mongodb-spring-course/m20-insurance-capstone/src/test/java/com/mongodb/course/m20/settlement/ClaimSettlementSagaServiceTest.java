package com.mongodb.course.m20.settlement;

import com.mongodb.course.m20.SharedContainersConfig;
import com.mongodb.course.m20.claim.model.ClaimStatus;
import com.mongodb.course.m20.claim.service.ClaimCommandService;
import com.mongodb.course.m20.infrastructure.saga.SagaLogRepository;
import com.mongodb.course.m20.infrastructure.saga.SagaStatus;
import com.mongodb.course.m20.policy.AutoPolicy;
import com.mongodb.course.m20.policy.HealthPolicy;
import com.mongodb.course.m20.policy.Policy;
import com.mongodb.course.m20.policy.PolicyService;
import com.mongodb.course.m20.projection.ClaimQueryService;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class ClaimSettlementSagaServiceTest {

    @Autowired private ClaimSettlementSagaService sagaService;
    @Autowired private ClaimCommandService claimCommandService;
    @Autowired private PolicyService policyService;
    @Autowired private SagaLogRepository sagaLogRepository;
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
    void successfulSettlement() {
        var policy = new AutoPolicy("pol-s1", "POL-S01", "Wang",
                new BigDecimal("10000"), new BigDecimal("500000"), "Sedan");
        policyService.save(policy);

        claimCommandService.fileClaim("CLM-S01", "pol-s1", "Wang", "AUTO",
                new BigDecimal("50000"), "Accident");
        claimCommandService.investigate("CLM-S01", "Inspector", "Verified", "LOW");
        claimCommandService.assess("CLM-S01", new BigDecimal("30000"), "Fair");

        String sagaId = sagaService.settle("CLM-S01", "pol-s1", "AUTO", new BigDecimal("30000"));

        var sagaLog = sagaLogRepository.findById(sagaId);
        assertThat(sagaLog).isPresent();
        assertThat(sagaLog.get().status()).isEqualTo(SagaStatus.COMPLETED);

        var claim = claimCommandService.loadClaim("CLM-S01");
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.PAID);

        var updatedPolicy = policyService.findById("pol-s1");
        assertThat(updatedPolicy).isPresent();
        assertThat(updatedPolicy.get().getTotalClaimsPaid()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(((AutoPolicy) updatedPolicy.get()).getAccidentCount()).isEqualTo(1);
    }

    @Test
    void settlementFailsOnFraudCheck() {
        var policy = new HealthPolicy("pol-s2", "POL-S02", "Li",
                new BigDecimal("8000"), new BigDecimal("1000000"), "STANDARD");
        policyService.save(policy);

        // Pre-populate statistics: 6 filed, 1 approved (approval rate < 30%)
        var statsQuery = Query.query(Criteria.where("_id").is("HEALTH"));
        var statsUpdate = new Update()
                .set("totalClaims", 6)
                .set("filedCount", 6)
                .set("approvedCount", 1)
                .set("rejectedCount", 4)
                .set("paidCount", 0)
                .set("investigatedCount", 5)
                .set("assessedCount", 3)
                .set("totalClaimedAmount", new Decimal128(new BigDecimal("500000")))
                .set("totalApprovedAmount", new Decimal128(new BigDecimal("50000")))
                .set("totalPaidAmount", new Decimal128(BigDecimal.ZERO));
        mongoTemplate.upsert(statsQuery, statsUpdate, "m20_claim_statistics");

        claimCommandService.fileClaim("CLM-S02", "pol-s2", "Li", "HEALTH",
                new BigDecimal("100000"), "Medical expense");
        claimCommandService.investigate("CLM-S02", "Inspector", "Suspicious", "MEDIUM");
        claimCommandService.assess("CLM-S02", new BigDecimal("80000"), "Assessed");

        String sagaId = sagaService.settle("CLM-S02", "pol-s2", "HEALTH", new BigDecimal("80000"));

        var sagaLog = sagaLogRepository.findById(sagaId);
        assertThat(sagaLog).isPresent();
        assertThat(sagaLog.get().status()).isEqualTo(SagaStatus.COMPENSATED);
    }

    @Test
    void sagaLogRecordsAllFourSteps() {
        var policy = new AutoPolicy("pol-s3", "POL-S03", "Zhang",
                new BigDecimal("12000"), new BigDecimal("800000"), "Van");
        policyService.save(policy);

        claimCommandService.fileClaim("CLM-S03", "pol-s3", "Zhang", "AUTO",
                new BigDecimal("60000"), "Collision");
        claimCommandService.investigate("CLM-S03", "Inspector", "Clear", "LOW");
        claimCommandService.assess("CLM-S03", new BigDecimal("50000"), "OK");

        String sagaId = sagaService.settle("CLM-S03", "pol-s3", "AUTO", new BigDecimal("50000"));

        var sagaLog = sagaLogRepository.findById(sagaId);
        assertThat(sagaLog).isPresent();
        assertThat(sagaLog.get().steps()).hasSize(4);
        assertThat(sagaLog.get().steps().stream().map(s -> s.stepName()).toList())
                .containsExactly("FRAUD_CHECK", "APPROVE_CLAIM", "UPDATE_POLICY", "NOTIFY_SETTLEMENT");
    }
}
