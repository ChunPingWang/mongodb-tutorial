package com.mongodb.course.m13.banking.command;

import com.mongodb.course.m13.banking.event.AccountEvent;
import com.mongodb.course.m13.banking.projection.AccountSummaryProjector;
import com.mongodb.course.m13.banking.projection.TransactionHistoryProjector;
import com.mongodb.course.m13.infrastructure.EventStore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BankAccountCommandService {

    private static final String COLLECTION = "m13_account_events";

    private final EventStore eventStore;
    private final AccountSummaryProjector accountSummaryProjector;
    private final TransactionHistoryProjector transactionHistoryProjector;

    public BankAccountCommandService(EventStore eventStore,
                                     AccountSummaryProjector accountSummaryProjector,
                                     TransactionHistoryProjector transactionHistoryProjector) {
        this.eventStore = eventStore;
        this.accountSummaryProjector = accountSummaryProjector;
        this.transactionHistoryProjector = transactionHistoryProjector;
    }

    public BankAccount openAccount(String accountId, String holder, BigDecimal initialBalance, String currency) {
        var account = BankAccount.open(accountId, holder, initialBalance, currency);
        var events = persistEvents(account);
        events.forEach(this::projectEvent);
        return account;
    }

    public BankAccount deposit(String accountId, BigDecimal amount, String description) {
        var account = loadAccount(accountId);
        account.deposit(amount, description);
        var events = persistEvents(account);
        events.forEach(this::projectEvent);
        return account;
    }

    public BankAccount withdraw(String accountId, BigDecimal amount, String description) {
        var account = loadAccount(accountId);
        account.withdraw(amount, description);
        var events = persistEvents(account);
        events.forEach(this::projectEvent);
        return account;
    }

    public BankAccount transfer(String accountId, BigDecimal amount, String targetAccountId, String description) {
        var account = loadAccount(accountId);
        account.transfer(amount, targetAccountId, description);
        var events = persistEvents(account);
        events.forEach(this::projectEvent);
        return account;
    }

    private BankAccount loadAccount(String accountId) {
        var events = eventStore.loadEvents(accountId, AccountEvent.class, COLLECTION);
        if (events.isEmpty()) {
            throw new IllegalArgumentException("Account not found: " + accountId);
        }
        return BankAccount.replayFrom(events);
    }

    private List<AccountEvent> persistEvents(BankAccount account) {
        var events = List.copyOf(account.getUncommittedEvents());
        eventStore.appendAll(events, COLLECTION);
        account.clearUncommittedEvents();
        return events;
    }

    private void projectEvent(AccountEvent event) {
        accountSummaryProjector.project(event);
        transactionHistoryProjector.project(event);
    }
}
