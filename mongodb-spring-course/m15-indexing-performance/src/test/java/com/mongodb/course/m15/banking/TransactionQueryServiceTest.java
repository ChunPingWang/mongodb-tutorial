package com.mongodb.course.m15.banking;

import com.mongodb.course.m15.SharedContainersConfig;
import com.mongodb.course.m15.index.ExplainAnalyzer;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class TransactionQueryServiceTest {

    private static final String COLLECTION = "m15_transactions";

    @Autowired
    private TransactionQueryService queryService;

    @Autowired
    private TransactionDataGenerator dataGenerator;

    @Autowired
    private ExplainAnalyzer explainAnalyzer;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.remove(new Query(), COLLECTION);
        mongoTemplate.indexOps(COLLECTION).dropAllIndexes();
        dataGenerator.generateTransactions(2000, 10);
    }

    @Test
    void findByAccountAndDateRange_usesCompoundIndex() {
        mongoTemplate.indexOps(COLLECTION).ensureIndex(
                new Index()
                        .on("accountId", Sort.Direction.ASC)
                        .on("transactionDate", Sort.Direction.ASC));

        var from = Instant.parse("2024-03-01T00:00:00Z");
        var to = Instant.parse("2024-06-01T00:00:00Z");

        var results = queryService.findByAccountAndDateRange("ACC-000001", from, to);
        assertThat(results).isNotEmpty();

        var explain = explainAnalyzer.explain(COLLECTION,
                new Document("accountId", "ACC-000001")
                        .append("transactionDate", new Document("$gte", from).append("$lte", to)));
        assertThat(explain.stage()).isEqualTo("IXSCAN");
    }

    @Test
    void findByAccountTypeAndDateRange_usesFullEsrIndex() {
        // ESR: Equality (accountId, type) + Sort (n/a) + Range (transactionDate)
        mongoTemplate.indexOps(COLLECTION).ensureIndex(
                new Index()
                        .on("accountId", Sort.Direction.ASC)
                        .on("type", Sort.Direction.ASC)
                        .on("transactionDate", Sort.Direction.ASC));

        var from = Instant.parse("2024-01-01T00:00:00Z");
        var to = Instant.parse("2024-12-31T00:00:00Z");

        var results = queryService.findByAccountTypeAndDateRange(
                "ACC-000001", TransactionType.DEPOSIT, from, to);
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(t -> t.type() == TransactionType.DEPOSIT);

        var explain = explainAnalyzer.explain(COLLECTION,
                new Document("accountId", "ACC-000001")
                        .append("type", "DEPOSIT")
                        .append("transactionDate", new Document("$gte", from).append("$lte", to)));
        assertThat(explain.stage()).isEqualTo("IXSCAN");
        assertThat(explain.docsExamined()).isEqualTo(explain.nReturned());
    }

    @Test
    void findRecentByAccount_sortsByDateDescending() {
        mongoTemplate.indexOps(COLLECTION).ensureIndex(
                new Index()
                        .on("accountId", Sort.Direction.ASC)
                        .on("transactionDate", Sort.Direction.DESC));

        var results = queryService.findRecentByAccount("ACC-000001", 5);
        assertThat(results).hasSize(5);

        for (int i = 0; i < results.size() - 1; i++) {
            assertThat(results.get(i).transactionDate())
                    .isAfterOrEqualTo(results.get(i + 1).transactionDate());
        }
    }

    @Test
    void coveredQuery_noDocsExamined() {
        mongoTemplate.indexOps(COLLECTION).ensureIndex(
                new Index()
                        .on("accountId", Sort.Direction.ASC)
                        .on("amount", Sort.Direction.ASC));

        var docs = queryService.findAccountAmountOnly("ACC-000001");
        assertThat(docs).isNotEmpty();

        var explain = explainAnalyzer.explain(COLLECTION,
                new Document("accountId", "ACC-000001"),
                new Document("accountId", 1).append("amount", 1).append("_id", 0));
        assertThat(explain.isIndexOnly()).isTrue();
        assertThat(explain.docsExamined()).isZero();
    }

    @Test
    void countByAccount_returnsCorrectCount() {
        long count = queryService.countByAccount("ACC-000001");
        // 2000 transactions / 10 accounts = 200 per account
        assertThat(count).isEqualTo(200);
    }
}
