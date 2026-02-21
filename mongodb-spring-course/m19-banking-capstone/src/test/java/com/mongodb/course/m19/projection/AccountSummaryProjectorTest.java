package com.mongodb.course.m19.projection;

import com.mongodb.course.m19.SharedContainersConfig;
import com.mongodb.course.m19.account.service.AccountCommandService;
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
class AccountSummaryProjectorTest {

    @Autowired private AccountCommandService accountCommandService;
    @Autowired private DashboardQueryService dashboardQueryService;
    @Autowired private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.remove(new Query(), "m19_account_events");
        mongoTemplate.remove(new Query(), "m19_snapshots");
        mongoTemplate.remove(new Query(), "m19_account_summaries");
        mongoTemplate.remove(new Query(), "m19_transaction_ledger");
    }

    @Test
    void projectionReflectsAllEvents() {
        accountCommandService.openAccount("PROJ-01", "Alice", new BigDecimal("10000"), "TWD");
        accountCommandService.deposit("PROJ-01", new BigDecimal("5000"), "存款1");
        accountCommandService.deposit("PROJ-01", new BigDecimal("3000"), "存款2");
        accountCommandService.withdraw("PROJ-01", new BigDecimal("2000"), "提款1");

        var summary = dashboardQueryService.getAccountSummary("PROJ-01");
        assertThat(summary).isNotNull();
        assertThat(summary.currentBalance()).isEqualByComparingTo(new BigDecimal("16000"));
        assertThat(summary.totalTransactions()).isEqualTo(4);
        assertThat(summary.depositCount()).isEqualTo(2);
        assertThat(summary.withdrawalCount()).isEqualTo(1);
    }

    @Test
    void topAccountsByBalance() {
        accountCommandService.openAccount("RANK-01", "Alice", new BigDecimal("50000"), "TWD");
        accountCommandService.openAccount("RANK-02", "Bob", new BigDecimal("80000"), "TWD");
        accountCommandService.openAccount("RANK-03", "Charlie", new BigDecimal("30000"), "TWD");

        var top2 = dashboardQueryService.topAccountsByBalance(2);
        assertThat(top2).hasSize(2);
        assertThat(top2.get(0).accountId()).isEqualTo("RANK-02");
        assertThat(top2.get(1).accountId()).isEqualTo("RANK-01");
    }
}
