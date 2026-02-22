package com.mongodb.course.m13.banking.projection;

import com.mongodb.course.m13.SharedContainersConfig;
import com.mongodb.course.m13.banking.event.AccountOpened;
import com.mongodb.course.m13.banking.event.FundsDeposited;
import com.mongodb.course.m13.banking.event.FundsWithdrawn;
import com.mongodb.course.m13.banking.readmodel.AccountSummaryDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class AccountSummaryProjectorTest {

    @Autowired
    private AccountSummaryProjector projector;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanup() {
        mongoTemplate.remove(new Query(), "m13_account_summaries");
    }

    @Test
    void projectAccountOpened_createsInitialSummary() {
        var event = new AccountOpened(UUID.randomUUID().toString(), "ACC-001", 1,
                Instant.now(), "Alice", new BigDecimal("10000"), "TWD");

        projector.project(event);

        var doc = mongoTemplate.findById("ACC-001", AccountSummaryDocument.class, "m13_account_summaries");
        assertThat(doc).isNotNull();
        assertThat(doc.accountHolder()).isEqualTo("Alice");
        assertThat(doc.currentBalance()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(doc.totalTransactionCount()).isEqualTo(1);
        assertThat(doc.depositCount()).isZero();
    }

    @Test
    void projectFundsDeposited_updatesBalanceAndCounts() {
        var opened = new AccountOpened(UUID.randomUUID().toString(), "ACC-002", 1,
                Instant.now(), "Bob", new BigDecimal("5000"), "TWD");
        var deposited = new FundsDeposited(UUID.randomUUID().toString(), "ACC-002", 2,
                Instant.now(), new BigDecimal("3000"), "Salary");

        projector.project(opened);
        projector.project(deposited);

        var doc = mongoTemplate.findById("ACC-002", AccountSummaryDocument.class, "m13_account_summaries");
        assertThat(doc).isNotNull();
        assertThat(doc.currentBalance()).isEqualByComparingTo(new BigDecimal("8000"));
        assertThat(doc.totalTransactionCount()).isEqualTo(2);
        assertThat(doc.depositCount()).isEqualTo(1);
        assertThat(doc.totalDeposited()).isEqualByComparingTo(new BigDecimal("3000"));
    }

    @Test
    void projectMultipleEvents_maintainsCorrectState() {
        var opened = new AccountOpened(UUID.randomUUID().toString(), "ACC-003", 1,
                Instant.now(), "Carol", new BigDecimal("10000"), "TWD");
        var deposited = new FundsDeposited(UUID.randomUUID().toString(), "ACC-003", 2,
                Instant.now(), new BigDecimal("5000"), "Bonus");
        var withdrawn = new FundsWithdrawn(UUID.randomUUID().toString(), "ACC-003", 3,
                Instant.now(), new BigDecimal("2000"), "Rent");

        projector.project(opened);
        projector.project(deposited);
        projector.project(withdrawn);

        var doc = mongoTemplate.findById("ACC-003", AccountSummaryDocument.class, "m13_account_summaries");
        assertThat(doc).isNotNull();
        assertThat(doc.currentBalance()).isEqualByComparingTo(new BigDecimal("13000"));
        assertThat(doc.totalTransactionCount()).isEqualTo(3);
        assertThat(doc.depositCount()).isEqualTo(1);
        assertThat(doc.withdrawalCount()).isEqualTo(1);
        assertThat(doc.projectedVersion()).isEqualTo(3);
    }

    @Test
    void rebuildAll_recreatesFromScratch() {
        var opened = new AccountOpened(UUID.randomUUID().toString(), "ACC-004", 1,
                Instant.now(), "Dave", new BigDecimal("20000"), "TWD");
        var deposited = new FundsDeposited(UUID.randomUUID().toString(), "ACC-004", 2,
                Instant.now(), new BigDecimal("5000"), "Transfer");
        projector.project(opened);
        projector.project(deposited);

        projector.rebuildAll(List.of(opened, deposited));

        var doc = mongoTemplate.findById("ACC-004", AccountSummaryDocument.class, "m13_account_summaries");
        assertThat(doc).isNotNull();
        assertThat(doc.currentBalance()).isEqualByComparingTo(new BigDecimal("25000"));
        assertThat(doc.totalTransactionCount()).isEqualTo(2);
    }
}
