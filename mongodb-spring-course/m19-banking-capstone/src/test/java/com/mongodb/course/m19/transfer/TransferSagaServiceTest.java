package com.mongodb.course.m19.transfer;

import com.mongodb.course.m19.SharedContainersConfig;
import com.mongodb.course.m19.account.service.AccountCommandService;
import com.mongodb.course.m19.infrastructure.saga.SagaLog;
import com.mongodb.course.m19.infrastructure.saga.SagaLogRepository;
import com.mongodb.course.m19.infrastructure.saga.SagaStatus;
import com.mongodb.course.m19.projection.DashboardQueryService;
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
class TransferSagaServiceTest {

    @Autowired private TransferSagaService transferSagaService;
    @Autowired private AccountCommandService accountCommandService;
    @Autowired private DashboardQueryService dashboardQueryService;
    @Autowired private SagaLogRepository sagaLogRepository;
    @Autowired private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.remove(new Query(), "m19_account_events");
        mongoTemplate.remove(new Query(), "m19_snapshots");
        mongoTemplate.remove(new Query(), "m19_account_summaries");
        mongoTemplate.remove(new Query(), "m19_transaction_ledger");
        mongoTemplate.remove(new Query(), "m19_transfer_saga_logs");
    }

    @Test
    void successfulTransfer() {
        accountCommandService.openAccount("TRF-T01", "Alice", new BigDecimal("50000"), "TWD");
        accountCommandService.openAccount("TRF-T02", "Bob", new BigDecimal("10000"), "TWD");

        String sagaId = transferSagaService.transfer("TRF-T01", "TRF-T02", new BigDecimal("20000"));

        assertThat(dashboardQueryService.getAccountBalance("TRF-T01"))
                .isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(dashboardQueryService.getAccountBalance("TRF-T02"))
                .isEqualByComparingTo(new BigDecimal("30000"));

        var sagaLog = sagaLogRepository.findById(sagaId).orElseThrow();
        assertThat(sagaLog.status()).isEqualTo(SagaStatus.COMPLETED);
    }

    @Test
    void failedTransferCompensates() {
        accountCommandService.openAccount("TRF-T03", "Alice", new BigDecimal("5000"), "TWD");
        accountCommandService.openAccount("TRF-T04", "Bob", new BigDecimal("10000"), "TWD");

        String sagaId = transferSagaService.transfer("TRF-T03", "TRF-T04", new BigDecimal("20000"));

        var sagaLog = sagaLogRepository.findById(sagaId).orElseThrow();
        assertThat(sagaLog.status()).isEqualTo(SagaStatus.COMPENSATED);

        assertThat(dashboardQueryService.getAccountBalance("TRF-T03"))
                .isEqualByComparingTo(new BigDecimal("5000"));
    }

    @Test
    void sagaLogRecordsAllSteps() {
        accountCommandService.openAccount("TRF-T05", "Alice", new BigDecimal("100000"), "TWD");
        accountCommandService.openAccount("TRF-T06", "Bob", new BigDecimal("10000"), "TWD");

        String sagaId = transferSagaService.transfer("TRF-T05", "TRF-T06", new BigDecimal("5000"));

        var sagaLog = sagaLogRepository.findById(sagaId).orElseThrow();
        assertThat(sagaLog.steps()).hasSize(3);
        assertThat(sagaLog.steps().stream().map(s -> s.stepName()))
                .containsExactly("DEBIT_SOURCE", "CREDIT_TARGET", "RECORD_TRANSFER");
    }
}
