package com.mongodb.course.m17.banking;

import com.mongodb.course.m17.SharedContainersConfig;
import com.mongodb.course.m17.observability.SlowQueryDetector;
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
class SlowQueryDetectorTest {

    @Autowired
    private SlowQueryDetector slowQueryDetector;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.remove(new Query(), "m17_transactions");
        slowQueryDetector.clear();
    }

    @Test
    void thresholdZero_capturesAllQueries() {
        slowQueryDetector.setThresholdMs(0);

        transactionService.create("ACC-001", 5000, "DEPOSIT");
        transactionService.findAll();

        assertThat(slowQueryDetector.getCapturedCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void thresholdVeryHigh_capturesNothing() {
        slowQueryDetector.setThresholdMs(999999);

        transactionService.create("ACC-002", 3000, "DEPOSIT");
        transactionService.findByAccountId("ACC-002");

        assertThat(slowQueryDetector.getCapturedCount()).isZero();
    }

    @Test
    void capturedEntry_containsCommandName() {
        slowQueryDetector.setThresholdMs(0);

        transactionService.findByAccountId("ACC-003");

        assertThat(slowQueryDetector.getCapturedQueries())
                .anyMatch(entry -> "find".equals(entry.commandName()));
    }

    @Test
    void capturedEntry_containsDatabaseName() {
        slowQueryDetector.setThresholdMs(0);

        transactionService.create("ACC-004", 8000, "DEPOSIT");

        assertThat(slowQueryDetector.getCapturedQueries())
                .allMatch(entry -> entry.databaseName() != null && !entry.databaseName().isEmpty());
    }

    @Test
    void clear_resetsCapturedQueries() {
        slowQueryDetector.setThresholdMs(0);

        transactionService.create("ACC-005", 1000, "DEPOSIT");
        assertThat(slowQueryDetector.getCapturedCount()).isGreaterThan(0);

        slowQueryDetector.clear();
        assertThat(slowQueryDetector.getCapturedCount()).isZero();
    }
}
