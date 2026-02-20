package com.mongodb.course.m12.banking;

import com.mongodb.course.m12.banking.event.*;
import com.mongodb.course.m12.banking.model.BankAccount;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BankAccountTest {

    @Test
    void open_createsAccountWithInitialBalance() {
        var account = BankAccount.open("ACC-001", "王小明", new BigDecimal("10000"), "TWD");

        assertThat(account.getAccountId()).isEqualTo("ACC-001");
        assertThat(account.getAccountHolder()).isEqualTo("王小明");
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(account.getCurrency()).isEqualTo("TWD");
        assertThat(account.getVersion()).isEqualTo(1);
        assertThat(account.getUncommittedEvents()).hasSize(1);
        assertThat(account.getUncommittedEvents().getFirst()).isInstanceOf(AccountOpened.class);
    }

    @Test
    void open_negativeBalance_throwsException() {
        assertThatThrownBy(() -> BankAccount.open("ACC-001", "王小明", new BigDecimal("-1000"), "TWD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    @Test
    void deposit_increasesBalance() {
        var account = BankAccount.open("ACC-001", "王小明", new BigDecimal("10000"), "TWD");
        account.clearUncommittedEvents();

        account.deposit(new BigDecimal("5000"), "薪資入帳");

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("15000"));
        assertThat(account.getVersion()).isEqualTo(2);
        assertThat(account.getUncommittedEvents()).hasSize(1);
        assertThat(account.getUncommittedEvents().getFirst()).isInstanceOf(FundsDeposited.class);
    }

    @Test
    void withdraw_decreasesBalance() {
        var account = BankAccount.open("ACC-001", "王小明", new BigDecimal("10000"), "TWD");
        account.clearUncommittedEvents();

        account.withdraw(new BigDecimal("3000"), "ATM提款");

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("7000"));
        assertThat(account.getVersion()).isEqualTo(2);
    }

    @Test
    void withdraw_insufficientFunds_throwsException() {
        var account = BankAccount.open("ACC-001", "王小明", new BigDecimal("10000"), "TWD");

        assertThatThrownBy(() -> account.withdraw(new BigDecimal("20000"), "大額提款"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    void replayFrom_rebuildsCorrectState() {
        String id = "ACC-001";
        var events = List.<AccountEvent>of(
                new AccountOpened(UUID.randomUUID().toString(), id, 1, Instant.now(),
                        "王小明", new BigDecimal("10000"), "TWD"),
                new FundsDeposited(UUID.randomUUID().toString(), id, 2, Instant.now(),
                        new BigDecimal("5000"), "存款"),
                new FundsWithdrawn(UUID.randomUUID().toString(), id, 3, Instant.now(),
                        new BigDecimal("2000"), "提款"));

        var account = BankAccount.replayFrom(events);

        assertThat(account.getAccountId()).isEqualTo(id);
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("13000"));
        assertThat(account.getVersion()).isEqualTo(3);
        assertThat(account.getUncommittedEvents()).isEmpty();
    }
}
