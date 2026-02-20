package com.mongodb.course.m13.banking;

import com.mongodb.course.m13.banking.command.BankAccount;
import com.mongodb.course.m13.banking.event.AccountOpened;
import com.mongodb.course.m13.banking.event.FundsDeposited;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BankAccountTest {

    @Test
    void open_createsAccountWithCorrectState() {
        var account = BankAccount.open("ACC-001", "Alice", new BigDecimal("10000"), "TWD");

        assertThat(account.getAccountId()).isEqualTo("ACC-001");
        assertThat(account.getAccountHolder()).isEqualTo("Alice");
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(account.getCurrency()).isEqualTo("TWD");
        assertThat(account.getVersion()).isEqualTo(1);
        assertThat(account.getUncommittedEvents()).hasSize(1);
        assertThat(account.getUncommittedEvents().getFirst()).isInstanceOf(AccountOpened.class);
    }

    @Test
    void deposit_producesEvent() {
        var account = BankAccount.open("ACC-002", "Bob", new BigDecimal("5000"), "TWD");
        account.clearUncommittedEvents();

        account.deposit(new BigDecimal("3000"), "Salary");

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("8000"));
        assertThat(account.getVersion()).isEqualTo(2);
        assertThat(account.getUncommittedEvents()).hasSize(1);
        assertThat(account.getUncommittedEvents().getFirst()).isInstanceOf(FundsDeposited.class);
    }

    @Test
    void withdraw_insufficientFunds_throws() {
        var account = BankAccount.open("ACC-003", "Carol", new BigDecimal("1000"), "TWD");

        assertThatThrownBy(() -> account.withdraw(new BigDecimal("5000"), "Overdraft attempt"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient funds");
    }
}
