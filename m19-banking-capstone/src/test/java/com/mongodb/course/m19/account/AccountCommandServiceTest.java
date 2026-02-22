package com.mongodb.course.m19.account;

import com.mongodb.course.m19.SharedContainersConfig;
import com.mongodb.course.m19.account.service.AccountCommandService;
import com.mongodb.course.m19.infrastructure.EventStore;
import com.mongodb.course.m19.account.event.AccountEvent;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(SharedContainersConfig.class)
class AccountCommandServiceTest {

    @Autowired
    private AccountCommandService accountCommandService;

    @Autowired
    private EventStore eventStore;

    @Autowired
    private DashboardQueryService dashboardQueryService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.remove(new Query(), "m19_account_events");
        mongoTemplate.remove(new Query(), "m19_snapshots");
        mongoTemplate.remove(new Query(), "m19_account_summaries");
        mongoTemplate.remove(new Query(), "m19_transaction_ledger");
    }

    @Test
    void openAccountAndDeposit() {
        accountCommandService.openAccount("ACC-T01", "王小明", new BigDecimal("10000"), "TWD");
        accountCommandService.deposit("ACC-T01", new BigDecimal("5000"), "薪資入帳");

        var account = accountCommandService.loadAccount("ACC-T01");
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("15000"));

        long eventCount = eventStore.countEvents("ACC-T01", "m19_account_events");
        assertThat(eventCount).isEqualTo(2);

        var summary = dashboardQueryService.getAccountSummary("ACC-T01");
        assertThat(summary).isNotNull();
        assertThat(summary.totalTransactions()).isEqualTo(2);
        assertThat(summary.currentBalance()).isEqualByComparingTo(new BigDecimal("15000"));
    }

    @Test
    void withdrawFailsOnInsufficientFunds() {
        accountCommandService.openAccount("ACC-T02", "李小華", new BigDecimal("5000"), "TWD");

        assertThatThrownBy(() ->
                accountCommandService.withdraw("ACC-T02", new BigDecimal("10000"), "大額提款"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    void accrueInterestAndCloseAccount() {
        accountCommandService.openAccount("ACC-T03", "張大為", new BigDecimal("10000"), "TWD");
        accountCommandService.accrueInterest("ACC-T03", new BigDecimal("500"));

        var account = accountCommandService.loadAccount("ACC-T03");
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("10500"));

        // Withdraw all to close
        accountCommandService.withdraw("ACC-T03", new BigDecimal("10500"), "全額提款");
        accountCommandService.closeAccount("ACC-T03");

        var closedAccount = accountCommandService.loadAccount("ACC-T03");
        assertThat(closedAccount.isClosed()).isTrue();

        var summary = dashboardQueryService.getAccountSummary("ACC-T03");
        assertThat(summary.closed()).isTrue();
        assertThat(summary.totalInterestEarned()).isEqualByComparingTo(new BigDecimal("500"));
    }
}
