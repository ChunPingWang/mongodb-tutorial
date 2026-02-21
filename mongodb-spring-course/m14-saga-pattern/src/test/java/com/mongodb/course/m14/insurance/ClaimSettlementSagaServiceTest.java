package com.mongodb.course.m14.insurance;

import com.mongodb.course.m14.SharedContainersConfig;
import com.mongodb.course.m14.insurance.model.Claim;
import com.mongodb.course.m14.insurance.model.ClaimSettlementStatus;
import com.mongodb.course.m14.insurance.model.Policy;
import com.mongodb.course.m14.insurance.service.ClaimSettlementSagaService;
import com.mongodb.course.m14.saga.SagaLogRepository;
import com.mongodb.course.m14.saga.SagaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class ClaimSettlementSagaServiceTest {

    @Autowired
    private ClaimSettlementSagaService claimSettlementSagaService;

    @Autowired
    private SagaLogRepository sagaLogRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanup() {
        mongoTemplate.remove(new Query(), "m14_saga_logs");
        mongoTemplate.remove(new Query(), "m14_claims");
        mongoTemplate.remove(new Query(), "m14_policies");
        mongoTemplate.remove(new Query(), "m14_payments");
        mongoTemplate.remove(new Query(), "m14_notifications");
    }

    @Test
    void settleClaim_happyPath_allStepsComplete() {
        mongoTemplate.save(new Policy("POL-001", "Alice", 500_000, 100_000));
        mongoTemplate.save(new Claim("CLM-001", "POL-001", "Alice", 50_000, ClaimSettlementStatus.PENDING));

        String sagaId = claimSettlementSagaService.settleClaim("CLM-001");

        var sagaLog = sagaLogRepository.findById(sagaId).orElseThrow();
        assertThat(sagaLog.status()).isEqualTo(SagaStatus.COMPLETED);

        var claim = mongoTemplate.findById("CLM-001", Claim.class);
        assertThat(claim.settlementStatus()).isEqualTo(ClaimSettlementStatus.NOTIFIED);

        var policy = mongoTemplate.findById("POL-001", Policy.class);
        assertThat(policy.paidClaimsTotal()).isEqualTo(150_000);
    }

    @Test
    void settleClaim_exceedsCoverage_compensatesApprovalAndPayment() {
        mongoTemplate.save(new Policy("POL-002", "Bob", 200_000, 180_000));
        mongoTemplate.save(new Claim("CLM-002", "POL-002", "Bob", 50_000, ClaimSettlementStatus.PENDING));

        String sagaId = claimSettlementSagaService.settleClaim("CLM-002");

        var sagaLog = sagaLogRepository.findById(sagaId).orElseThrow();
        assertThat(sagaLog.status()).isEqualTo(SagaStatus.COMPENSATED);

        var claim = mongoTemplate.findById("CLM-002", Claim.class);
        assertThat(claim.settlementStatus()).isEqualTo(ClaimSettlementStatus.PENDING);

        var policy = mongoTemplate.findById("POL-002", Policy.class);
        assertThat(policy.paidClaimsTotal()).isEqualTo(180_000);
    }

    @Test
    void settleClaim_notificationFails_compensatesPriorSteps() {
        mongoTemplate.save(new Policy("POL-003", "FAIL_Carol", 500_000, 0));
        mongoTemplate.save(new Claim("CLM-003", "POL-003", "FAIL_Carol", 30_000, ClaimSettlementStatus.PENDING));

        String sagaId = claimSettlementSagaService.settleClaim("CLM-003");

        var sagaLog = sagaLogRepository.findById(sagaId).orElseThrow();
        assertThat(sagaLog.status()).isEqualTo(SagaStatus.COMPENSATED);

        var claim = mongoTemplate.findById("CLM-003", Claim.class);
        assertThat(claim.settlementStatus()).isEqualTo(ClaimSettlementStatus.PENDING);

        var policy = mongoTemplate.findById("POL-003", Policy.class);
        assertThat(policy.paidClaimsTotal()).isEqualTo(0);
    }

    @Test
    void settleClaim_sagaLogTracksCompensation() {
        mongoTemplate.save(new Policy("POL-004", "FAIL_Dave", 500_000, 0));
        mongoTemplate.save(new Claim("CLM-004", "POL-004", "FAIL_Dave", 20_000, ClaimSettlementStatus.PENDING));

        String sagaId = claimSettlementSagaService.settleClaim("CLM-004");

        var sagaLog = sagaLogRepository.findById(sagaId).orElseThrow();
        assertThat(sagaLog.status()).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(sagaLog.steps()).hasSize(4);

        // Steps 0-2 succeeded then compensated, step 3 failed
        assertThat(sagaLog.steps().get(0).status()).isEqualTo("COMPENSATED");
        assertThat(sagaLog.steps().get(1).status()).isEqualTo("COMPENSATED");
        assertThat(sagaLog.steps().get(2).status()).isEqualTo("COMPENSATED");
        assertThat(sagaLog.steps().get(3).status()).isEqualTo("FAILED");
    }
}
