package com.mongodb.course.m13.banking.query;

import com.mongodb.course.m13.SharedContainersConfig;
import com.mongodb.course.m13.banking.command.BankAccountCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class BankAccountQueryServiceTest {

    @Autowired
    private BankAccountQueryService queryService;

    @Autowired
    private BankAccountCommandService commandService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanup() {
        mongoTemplate.remove(new Query(), "m13_account_events");
        mongoTemplate.remove(new Query(), "m13_account_summaries");
        mongoTemplate.remove(new Query(), "m13_transaction_history");
    }

    @Test
    void getTopAccountsByBalance_returnsOrderedResults() {
        commandService.openAccount("Q-ACC-01", "Alice", new BigDecimal("50000"), "TWD");
        commandService.openAccount("Q-ACC-02", "Bob", new BigDecimal("80000"), "TWD");
        commandService.openAccount("Q-ACC-03", "Carol", new BigDecimal("30000"), "TWD");

        var top2 = queryService.getTopAccountsByBalance(2);

        assertThat(top2).hasSize(2);
        assertThat(top2.get(0).accountId()).isEqualTo("Q-ACC-02");
        assertThat(top2.get(1).accountId()).isEqualTo("Q-ACC-01");
    }

    @Test
    void getTransactionHistory_withPagination() {
        commandService.openAccount("Q-ACC-04", "Dave", new BigDecimal("10000"), "TWD");
        commandService.deposit("Q-ACC-04", new BigDecimal("1000"), "Dep 1");
        commandService.deposit("Q-ACC-04", new BigDecimal("2000"), "Dep 2");
        commandService.withdraw("Q-ACC-04", new BigDecimal("500"), "Wd 1");

        var page0 = queryService.getTransactionHistory("Q-ACC-04", 0, 2);
        var page1 = queryService.getTransactionHistory("Q-ACC-04", 1, 2);

        assertThat(page0).hasSize(2);
        assertThat(page1).hasSize(2);
    }

    @Test
    void getRecentlyActiveAccounts_filtersCorrectly() {
        var before = Instant.now();
        commandService.openAccount("Q-ACC-05", "Eve", new BigDecimal("10000"), "TWD");
        commandService.openAccount("Q-ACC-06", "Frank", new BigDecimal("20000"), "TWD");

        var recent = queryService.getRecentlyActiveAccounts(before);

        assertThat(recent).hasSizeGreaterThanOrEqualTo(2);
        assertThat(recent).extracting("accountId").contains("Q-ACC-05", "Q-ACC-06");
    }
}
