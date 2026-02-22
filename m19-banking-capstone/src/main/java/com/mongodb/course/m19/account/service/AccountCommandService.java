package com.mongodb.course.m19.account.service;

import com.mongodb.course.m19.account.event.AccountEvent;
import com.mongodb.course.m19.account.model.BankAccount;
import com.mongodb.course.m19.infrastructure.EventStore;
import com.mongodb.course.m19.infrastructure.SnapshotDocument;
import com.mongodb.course.m19.projection.AccountSummaryProjector;
import com.mongodb.course.m19.projection.TransactionLedgerProjector;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AccountCommandService {

    private static final String ACCOUNT_EVENTS = "m19_account_events";
    private static final String AGGREGATE_TYPE = "BankAccount";
    private static final int SNAPSHOT_INTERVAL = 10;

    private final EventStore eventStore;
    private final AccountSummaryProjector summaryProjector;
    private final TransactionLedgerProjector ledgerProjector;

    public AccountCommandService(EventStore eventStore,
                                 AccountSummaryProjector summaryProjector,
                                 TransactionLedgerProjector ledgerProjector) {
        this.eventStore = eventStore;
        this.summaryProjector = summaryProjector;
        this.ledgerProjector = ledgerProjector;
    }

    public BankAccount openAccount(String accountId, String accountHolder,
                                   BigDecimal initialBalance, String currency) {
        var account = BankAccount.open(accountId, accountHolder, initialBalance, currency);
        saveAndProject(account);
        return account;
    }

    public BankAccount deposit(String accountId, BigDecimal amount, String description) {
        var account = loadAccount(accountId);
        account.deposit(amount, description);
        saveAndProject(account);
        return account;
    }

    public BankAccount withdraw(String accountId, BigDecimal amount, String description) {
        var account = loadAccount(accountId);
        account.withdraw(amount, description);
        saveAndProject(account);
        return account;
    }

    public BankAccount accrueInterest(String accountId, BigDecimal amount) {
        var account = loadAccount(accountId);
        account.accrueInterest(amount);
        saveAndProject(account);
        return account;
    }

    public BankAccount closeAccount(String accountId) {
        var account = loadAccount(accountId);
        account.close();
        saveAndProject(account);
        return account;
    }

    public BankAccount loadAccount(String accountId) {
        var snapshotOpt = eventStore.loadLatestSnapshot(accountId, AGGREGATE_TYPE);
        if (snapshotOpt.isPresent()) {
            var snapshot = snapshotOpt.get();
            var account = BankAccount.fromSnapshot(snapshot.state());
            var newEvents = eventStore.loadEventsAfterVersion(
                    accountId, snapshot.version(), AccountEvent.class, ACCOUNT_EVENTS);
            account.replayAfterSnapshot(newEvents);
            return account;
        }
        var events = eventStore.loadEvents(accountId, AccountEvent.class, ACCOUNT_EVENTS);
        return BankAccount.replayFrom(events);
    }

    private void saveAndProject(BankAccount account) {
        var events = List.copyOf(account.getUncommittedEvents());
        account.clearUncommittedEvents();

        eventStore.appendAll(events, ACCOUNT_EVENTS);

        for (var event : events) {
            summaryProjector.project(event);
            ledgerProjector.project(event);
        }

        if (account.getVersion() % SNAPSHOT_INTERVAL == 0) {
            var snapshot = new SnapshotDocument(
                    UUID.randomUUID().toString(),
                    account.getAccountId(),
                    AGGREGATE_TYPE,
                    account.getVersion(),
                    Instant.now(),
                    account.toSnapshot());
            eventStore.saveSnapshot(snapshot);
        }
    }
}
