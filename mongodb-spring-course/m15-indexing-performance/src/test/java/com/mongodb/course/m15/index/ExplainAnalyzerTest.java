package com.mongodb.course.m15.index;

import com.mongodb.course.m15.SharedContainersConfig;
import com.mongodb.course.m15.banking.Transaction;
import com.mongodb.course.m15.banking.TransactionType;
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
class ExplainAnalyzerTest {

    private static final String COLLECTION = "m15_transactions";

    @Autowired
    private ExplainAnalyzer explainAnalyzer;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.remove(new Query(), COLLECTION);
        mongoTemplate.indexOps(COLLECTION).dropAllIndexes();
        mongoTemplate.insert(Transaction.of("ACC-000001", Instant.now(),
                TransactionType.DEPOSIT, 1000L, "test", "salary"), COLLECTION);
    }

    @Test
    void explain_withIndex_returnsIxscan() {
        mongoTemplate.indexOps(COLLECTION).ensureIndex(
                new Index().on("accountId", Sort.Direction.ASC));

        var result = explainAnalyzer.explain(COLLECTION,
                new Document("accountId", "ACC-000001"));

        assertThat(result.stage()).isEqualTo("IXSCAN");
        assertThat(result.indexName()).contains("accountId");
    }

    @Test
    void explain_withoutIndex_returnsCollscan() {
        var result = explainAnalyzer.explain(COLLECTION,
                new Document("description", "test"));

        assertThat(result.stage()).isEqualTo("COLLSCAN");
        assertThat(result.indexName()).isNull();
    }
}
