package com.mongodb.course.m12.banking;

import com.mongodb.course.m12.SharedContainersConfig;
import com.mongodb.course.m12.banking.event.AccountOpened;
import com.mongodb.course.m12.banking.event.FundsDeposited;
import com.mongodb.course.m12.banking.event.FundsWithdrawn;
import com.mongodb.course.m12.banking.service.BankAccountService;
import com.mongodb.course.m12.infrastructure.EventStore;
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
class BankAccountServiceTest {

    @Autowired
    BankAccountService bankAccountService;

    @Autowired
    EventStore eventStore;

    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.remove(new Query(), "m12_account_events");
        mongoTemplate.remove(new Query(), "m12_snapshots");
    }

    @Test
    void openAccount_persistsEvent() {
        bankAccountService.openAccount("ACC-S01", "王小明", new BigDecimal("10000"), "TWD");

        var account = bankAccountService.loadAccount("ACC-S01");
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(account.getAccountHolder()).isEqualTo("王小明");

        var events = bankAccountService.getEventHistory("ACC-S01");
        assertThat(events).hasSize(1);
        assertThat(events.getFirst()).isInstanceOf(AccountOpened.class);
    }

    @Test
    void depositAndWithdraw_eventHistory() {
        bankAccountService.openAccount("ACC-S02", "王小明", new BigDecimal("10000"), "TWD");
        bankAccountService.deposit("ACC-S02", new BigDecimal("5000"), "存款");
        bankAccountService.withdraw("ACC-S02", new BigDecimal("2000"), "提款");

        var events = bankAccountService.getEventHistory("ACC-S02");
        assertThat(events).hasSize(3);
        assertThat(events.get(0)).isInstanceOf(AccountOpened.class);
        assertThat(events.get(1)).isInstanceOf(FundsDeposited.class);
        assertThat(events.get(2)).isInstanceOf(FundsWithdrawn.class);
    }

    @Test
    void loadAccount_fullReplay() {
        bankAccountService.openAccount("ACC-S03", "王小明", new BigDecimal("10000"), "TWD");
        bankAccountService.deposit("ACC-S03", new BigDecimal("1000"), "存款1");
        bankAccountService.deposit("ACC-S03", new BigDecimal("2000"), "存款2");
        bankAccountService.deposit("ACC-S03", new BigDecimal("3000"), "存款3");

        var account = bankAccountService.loadAccount("ACC-S03");
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("16000"));
        assertThat(account.getVersion()).isEqualTo(4);
    }

    @Test
    void snapshotCreatedAfterThreshold() {
        bankAccountService.openAccount("ACC-S04", "王小明", new BigDecimal("10000"), "TWD");
        bankAccountService.deposit("ACC-S04", new BigDecimal("1000"), "存款1");
        bankAccountService.deposit("ACC-S04", new BigDecimal("1000"), "存款2");
        bankAccountService.deposit("ACC-S04", new BigDecimal("1000"), "存款3");
        bankAccountService.deposit("ACC-S04", new BigDecimal("1000"), "存款4");
        // 5 events total → triggers snapshot

        var snapshot = eventStore.loadLatestSnapshot("ACC-S04", "BankAccount");
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().version()).isEqualTo(5);
        assertThat(snapshot.get().state().get("balance")).isEqualTo("14000");
    }

    @Test
    void loadAccount_usesSnapshotPlusIncremental() {
        bankAccountService.openAccount("ACC-S05", "王小明", new BigDecimal("10000"), "TWD");
        bankAccountService.deposit("ACC-S05", new BigDecimal("1000"), "存款1");
        bankAccountService.deposit("ACC-S05", new BigDecimal("1000"), "存款2");
        bankAccountService.deposit("ACC-S05", new BigDecimal("1000"), "存款3");
        bankAccountService.deposit("ACC-S05", new BigDecimal("1000"), "存款4");
        // snapshot at version 5, balance=14000

        bankAccountService.deposit("ACC-S05", new BigDecimal("2000"), "存款5");
        bankAccountService.deposit("ACC-S05", new BigDecimal("3000"), "存款6");

        var account = bankAccountService.loadAccount("ACC-S05");
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("19000"));
        assertThat(account.getVersion()).isEqualTo(7);
    }
}
