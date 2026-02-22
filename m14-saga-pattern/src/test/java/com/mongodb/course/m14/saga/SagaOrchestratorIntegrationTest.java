package com.mongodb.course.m14.saga;

import com.mongodb.course.m14.SharedContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class SagaOrchestratorIntegrationTest {

    @Autowired
    private SagaOrchestrator orchestrator;

    @Autowired
    private SagaLogRepository sagaLogRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanup() {
        mongoTemplate.remove(new Query(), "m14_saga_logs");
    }

    @Test
    void execute_allStepsSucceed_statusIsCompleted() {
        var steps = List.<SagaStep>of(
                successStep("STEP_A"),
                successStep("STEP_B")
        );

        String sagaId = orchestrator.execute("TEST", steps, new SagaContext());

        var log = sagaLogRepository.findById(sagaId).orElseThrow();
        assertThat(log.status()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(log.steps()).allMatch(s -> "SUCCEEDED".equals(s.status()));
    }

    @Test
    void execute_secondStepFails_compensatesFirstStep() {
        var compensated = new ArrayList<String>();
        var steps = List.<SagaStep>of(
                trackableSuccessStep("STEP_A", compensated),
                failStep("STEP_B")
        );

        String sagaId = orchestrator.execute("TEST", steps, new SagaContext());

        var log = sagaLogRepository.findById(sagaId).orElseThrow();
        assertThat(log.status()).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(log.steps().get(0).status()).isEqualTo("COMPENSATED");
        assertThat(log.steps().get(1).status()).isEqualTo("FAILED");
        assertThat(compensated).containsExactly("STEP_A");
    }

    @Test
    void execute_thirdStepFails_compensatesInReverseOrder() {
        var compensated = new ArrayList<String>();
        var steps = List.<SagaStep>of(
                trackableSuccessStep("STEP_A", compensated),
                trackableSuccessStep("STEP_B", compensated),
                failStep("STEP_C")
        );

        String sagaId = orchestrator.execute("TEST", steps, new SagaContext());

        var log = sagaLogRepository.findById(sagaId).orElseThrow();
        assertThat(log.status()).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(compensated).containsExactly("STEP_B", "STEP_A");
    }

    @Test
    void execute_compensationAlsoFails_statusIsFailed() {
        var steps = List.<SagaStep>of(
                failOnCompensateStep("STEP_A"),
                failStep("STEP_B")
        );

        String sagaId = orchestrator.execute("TEST", steps, new SagaContext());

        var log = sagaLogRepository.findById(sagaId).orElseThrow();
        assertThat(log.status()).isEqualTo(SagaStatus.FAILED);
    }

    // --- Anonymous SagaStep implementations ---

    private SagaStep successStep(String name) {
        return new SagaStep() {
            @Override public String name() { return name; }
            @Override public void execute(SagaContext context) { }
            @Override public void compensate(SagaContext context) { }
        };
    }

    private SagaStep trackableSuccessStep(String name, List<String> compensated) {
        return new SagaStep() {
            @Override public String name() { return name; }
            @Override public void execute(SagaContext context) { }
            @Override public void compensate(SagaContext context) { compensated.add(name); }
        };
    }

    private SagaStep failStep(String name) {
        return new SagaStep() {
            @Override public String name() { return name; }
            @Override public void execute(SagaContext context) { throw new RuntimeException(name + " failed"); }
            @Override public void compensate(SagaContext context) { }
        };
    }

    private SagaStep failOnCompensateStep(String name) {
        return new SagaStep() {
            @Override public String name() { return name; }
            @Override public void execute(SagaContext context) { }
            @Override public void compensate(SagaContext context) { throw new RuntimeException("Compensation failed for " + name); }
        };
    }
}
