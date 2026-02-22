package com.mongodb.course.m13.banking.projection;

import com.mongodb.course.m13.SharedContainersConfig;
import com.mongodb.course.m13.banking.event.AccountOpened;
import com.mongodb.course.m13.banking.event.FundsDeposited;
import com.mongodb.course.m13.banking.event.FundsWithdrawn;
import com.mongodb.course.m13.banking.readmodel.TransactionHistoryDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class TransactionHistoryProjectorTest {

    @Autowired
    private TransactionHistoryProjector projector;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanup() {
        mongoTemplate.remove(new Query(), "m13_transaction_history");
    }

    @Test
    void projectAccountOpened_createsOpeningEntry() {
        var event = new AccountOpened(UUID.randomUUID().toString(), "ACC-001", 1,
                Instant.now(), "Alice", new BigDecimal("10000"), "TWD");

        projector.project(event);

        var docs = mongoTemplate.findAll(TransactionHistoryDocument.class, "m13_transaction_history");
        assertThat(docs).hasSize(1);
        assertThat(docs.getFirst().transactionType()).isEqualTo("OPENING");
        assertThat(docs.getFirst().balanceAfter()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    void projectFundsDeposited_createsEntryWithBalanceAfter() {
        var opened = new AccountOpened(UUID.randomUUID().toString(), "ACC-002", 1,
                Instant.now(), "Bob", new BigDecimal("5000"), "TWD");
        var deposited = new FundsDeposited(UUID.randomUUID().toString(), "ACC-002", 2,
                Instant.now(), new BigDecimal("3000"), "Salary");

        projector.project(opened);
        projector.project(deposited);

        var query = Query.query(Criteria.where("accountId").is("ACC-002")
                        .and("transactionType").is("DEPOSIT"));
        var doc = mongoTemplate.findOne(query, TransactionHistoryDocument.class, "m13_transaction_history");
        assertThat(doc).isNotNull();
        assertThat(doc.balanceAfter()).isEqualByComparingTo(new BigDecimal("8000"));
        assertThat(doc.amount()).isEqualByComparingTo(new BigDecimal("3000"));
    }

    @Test
    void projectMixedEvents_chronologicalOrder() {
        var now = Instant.now();
        var opened = new AccountOpened(UUID.randomUUID().toString(), "ACC-003", 1,
                now, "Carol", new BigDecimal("10000"), "TWD");
        var deposited = new FundsDeposited(UUID.randomUUID().toString(), "ACC-003", 2,
                now.plusMillis(100), new BigDecimal("5000"), "Bonus");
        var withdrawn = new FundsWithdrawn(UUID.randomUUID().toString(), "ACC-003", 3,
                now.plusMillis(200), new BigDecimal("2000"), "Rent");

        projector.project(opened);
        projector.project(deposited);
        projector.project(withdrawn);

        var query = Query.query(Criteria.where("accountId").is("ACC-003"))
                .with(Sort.by(Sort.Direction.ASC, "occurredAt"));
        var docs = mongoTemplate.find(query, TransactionHistoryDocument.class, "m13_transaction_history");
        assertThat(docs).hasSize(3);
        assertThat(docs.get(0).transactionType()).isEqualTo("OPENING");
        assertThat(docs.get(1).transactionType()).isEqualTo("DEPOSIT");
        assertThat(docs.get(2).transactionType()).isEqualTo("WITHDRAWAL");
        assertThat(docs.get(2).balanceAfter()).isEqualByComparingTo(new BigDecimal("13000"));
    }
}
