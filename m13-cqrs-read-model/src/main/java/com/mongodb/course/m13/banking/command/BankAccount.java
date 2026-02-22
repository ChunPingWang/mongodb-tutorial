package com.mongodb.course.m13.banking.command;

import com.mongodb.course.m13.banking.event.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class BankAccount {

    private String accountId;
    private String accountHolder;
    private BigDecimal balance;
    private String currency;
    private long version;

    private final List<AccountEvent> uncommittedEvents = new ArrayList<>();

    private BankAccount() {
    }

    public static BankAccount open(String accountId, String accountHolder,
                                   BigDecimal initialBalance, String currency) {
        if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
        var account = new BankAccount();
        var event = new AccountOpened(
                UUID.randomUUID().toString(), accountId, 1, Instant.now(),
                accountHolder, initialBalance, currency);
        account.apply(event);
        account.uncommittedEvents.add(event);
        return account;
    }

    public AccountEvent deposit(BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        var event = new FundsDeposited(
                UUID.randomUUID().toString(), accountId, version + 1, Instant.now(),
                amount, description);
        apply(event);
        uncommittedEvents.add(event);
        return event;
    }

    public AccountEvent withdraw(BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds: balance=" + balance + ", requested=" + amount);
        }
        var event = new FundsWithdrawn(
                UUID.randomUUID().toString(), accountId, version + 1, Instant.now(),
                amount, description);
        apply(event);
        uncommittedEvents.add(event);
        return event;
    }

    public AccountEvent transfer(BigDecimal amount, String targetAccountId, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds: balance=" + balance + ", requested=" + amount);
        }
        var event = new FundsTransferred(
                UUID.randomUUID().toString(), accountId, version + 1, Instant.now(),
                amount, targetAccountId, description);
        apply(event);
        uncommittedEvents.add(event);
        return event;
    }

    public static BankAccount replayFrom(List<AccountEvent> events) {
        if (events.isEmpty()) {
            throw new IllegalArgumentException("Cannot replay from empty event list");
        }
        var account = new BankAccount();
        events.forEach(account::apply);
        return account;
    }

    private void apply(AccountEvent event) {
        switch (event) {
            case AccountOpened e -> {
                this.accountId = e.aggregateId();
                this.accountHolder = e.accountHolder();
                this.balance = e.initialBalance();
                this.currency = e.currency();
            }
            case FundsDeposited e -> this.balance = this.balance.add(e.amount());
            case FundsWithdrawn e -> this.balance = this.balance.subtract(e.amount());
            case FundsTransferred e -> this.balance = this.balance.subtract(e.amount());
        }
        this.version = event.version();
    }

    public List<AccountEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    public void clearUncommittedEvents() {
        uncommittedEvents.clear();
    }

    public String getAccountId() { return accountId; }
    public String getAccountHolder() { return accountHolder; }
    public BigDecimal getBalance() { return balance; }
    public String getCurrency() { return currency; }
    public long getVersion() { return version; }
}
