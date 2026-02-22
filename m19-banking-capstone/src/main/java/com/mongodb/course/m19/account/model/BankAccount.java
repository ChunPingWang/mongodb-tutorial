package com.mongodb.course.m19.account.model;

import com.mongodb.course.m19.account.event.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class BankAccount {

    private String accountId;
    private String accountHolder;
    private BigDecimal balance;
    private String currency;
    private long version;
    private boolean closed;

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
        if (closed) {
            throw new IllegalStateException("Account is closed");
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
        if (closed) {
            throw new IllegalStateException("Account is closed");
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

    public AccountEvent transferOut(BigDecimal amount, String targetAccountId, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        if (closed) {
            throw new IllegalStateException("Account is closed");
        }
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds: balance=" + balance + ", requested=" + amount);
        }
        var event = new FundsTransferredOut(
                UUID.randomUUID().toString(), accountId, version + 1, Instant.now(),
                amount, targetAccountId, description);
        apply(event);
        uncommittedEvents.add(event);
        return event;
    }

    public void applyTransferIn(FundsTransferredIn event) {
        apply(event);
        uncommittedEvents.add(event);
    }

    public AccountEvent accrueInterest(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Interest amount must be positive");
        }
        if (closed) {
            throw new IllegalStateException("Account is closed");
        }
        var event = new InterestAccrued(
                UUID.randomUUID().toString(), accountId, version + 1, Instant.now(),
                amount);
        apply(event);
        uncommittedEvents.add(event);
        return event;
    }

    public AccountEvent close() {
        if (closed) {
            throw new IllegalStateException("Account is already closed");
        }
        if (balance.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Cannot close account with non-zero balance: " + balance);
        }
        var event = new AccountClosed(
                UUID.randomUUID().toString(), accountId, version + 1, Instant.now());
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

    public void replayAfterSnapshot(List<AccountEvent> events) {
        events.forEach(this::apply);
    }

    public Map<String, Object> toSnapshot() {
        var state = new LinkedHashMap<String, Object>();
        state.put("accountId", accountId);
        state.put("accountHolder", accountHolder);
        state.put("balance", balance.toPlainString());
        state.put("currency", currency);
        state.put("version", version);
        state.put("closed", closed);
        return state;
    }

    public static BankAccount fromSnapshot(Map<String, Object> state) {
        var account = new BankAccount();
        account.accountId = (String) state.get("accountId");
        account.accountHolder = (String) state.get("accountHolder");
        account.balance = new BigDecimal((String) state.get("balance"));
        account.currency = (String) state.get("currency");
        account.version = ((Number) state.get("version")).longValue();
        account.closed = Boolean.TRUE.equals(state.get("closed"));
        return account;
    }

    private void apply(AccountEvent event) {
        switch (event) {
            case AccountOpened e -> {
                this.accountId = e.aggregateId();
                this.accountHolder = e.accountHolder();
                this.balance = e.initialBalance();
                this.currency = e.currency();
                this.closed = false;
            }
            case FundsDeposited e -> this.balance = this.balance.add(e.amount());
            case FundsWithdrawn e -> this.balance = this.balance.subtract(e.amount());
            case FundsTransferredOut e -> this.balance = this.balance.subtract(e.amount());
            case FundsTransferredIn e -> this.balance = this.balance.add(e.amount());
            case InterestAccrued e -> this.balance = this.balance.add(e.amount());
            case AccountClosed e -> this.closed = true;
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
    public boolean isClosed() { return closed; }
}
