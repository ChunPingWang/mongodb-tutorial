package com.mongodb.course.m14.saga;

import com.mongodb.course.m14.SharedContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class SagaLogRepositoryTest {

    @Autowired
    private SagaLogRepository sagaLogRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanup() {
        mongoTemplate.remove(new Query(), "m14_saga_logs");
    }

    @Test
    void save_and_findById_roundTrip() {
        var sagaLog = SagaLog.create("saga-001", "TEST_SAGA", List.of("step1", "step2"), Map.of("key", "value"));
        sagaLogRepository.save(sagaLog);

        var found = sagaLogRepository.findById("saga-001");

        assertThat(found).isPresent();
        assertThat(found.get().sagaType()).isEqualTo("TEST_SAGA");
        assertThat(found.get().status()).isEqualTo(SagaStatus.STARTED);
        assertThat(found.get().steps()).hasSize(2);
        assertThat(found.get().steps().getFirst().stepName()).isEqualTo("step1");
        assertThat(found.get().steps().getFirst().status()).isEqualTo("PENDING");
    }

    @Test
    void updateStatus_persists() {
        var sagaLog = SagaLog.create("saga-002", "TEST_SAGA", List.of("step1"), Map.of());
        sagaLogRepository.save(sagaLog);

        sagaLogRepository.updateStatus("saga-002", SagaStatus.COMPLETED);

        var found = sagaLogRepository.findById("saga-002").orElseThrow();
        assertThat(found.status()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(found.completedAt()).isNotNull();
    }

    @Test
    void updateStep_persists() {
        var sagaLog = SagaLog.create("saga-003", "TEST_SAGA", List.of("step1", "step2"), Map.of());
        sagaLogRepository.save(sagaLog);

        var succeededLog = StepLog.pending("step1").succeeded();
        sagaLogRepository.updateStep("saga-003", 0, succeededLog);

        var found = sagaLogRepository.findById("saga-003").orElseThrow();
        assertThat(found.steps().getFirst().status()).isEqualTo("SUCCEEDED");
        assertThat(found.steps().getFirst().executedAt()).isNotNull();
        assertThat(found.currentStepIndex()).isEqualTo(0);
    }
}
