package com.mongodb.course.m12.banking.service;

import com.mongodb.course.m12.banking.event.AccountEvent;
import com.mongodb.course.m12.banking.model.BankAccount;
import com.mongodb.course.m12.infrastructure.EventStore;
import com.mongodb.course.m12.infrastructure.SnapshotDocument;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BankAccountService {

    static final int SNAPSHOT_THRESHOLD = 5;
    private static final String COLLECTION = "m12_account_events";
    private static final String AGGREGATE_TYPE = "BankAccount";

    private final EventStore eventStore;

    public BankAccountService(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    public BankAccount openAccount(String accountId, String accountHolder,
                                   BigDecimal initialBalance, String currency) {
        var account = BankAccount.open(accountId, accountHolder, initialBalance, currency);
        eventStore.appendAll(account.getUncommittedEvents().stream()
                .map(e -> (AccountEvent) e).toList(), COLLECTION);
        account.clearUncommittedEvents();
        return account;
    }

    public BankAccount deposit(String accountId, BigDecimal amount, String description) {
        var account = loadAccount(accountId);
        account.deposit(amount, description);
        persistAndSnapshot(account);
        return account;
    }

    public BankAccount withdraw(String accountId, BigDecimal amount, String description) {
        var account = loadAccount(accountId);
        account.withdraw(amount, description);
        persistAndSnapshot(account);
        return account;
    }

    public BankAccount transfer(String accountId, BigDecimal amount,
                                String targetAccountId, String description) {
        var account = loadAccount(accountId);
        account.transfer(amount, targetAccountId, description);
        persistAndSnapshot(account);
        return account;
    }

    public BankAccount loadAccount(String accountId) {
        var snapshot = eventStore.loadLatestSnapshot(accountId, AGGREGATE_TYPE);
        if (snapshot.isPresent()) {
            var account = BankAccount.fromSnapshot(snapshot.get().state());
            var incrementalEvents = eventStore.loadEventsAfterVersion(
                    accountId, snapshot.get().version(), AccountEvent.class, COLLECTION);
            account.replayAfterSnapshot(incrementalEvents);
            return account;
        }
        var events = eventStore.loadEvents(accountId, AccountEvent.class, COLLECTION);
        if (events.isEmpty()) {
            throw new IllegalArgumentException("Account not found: " + accountId);
        }
        return BankAccount.replayFrom(events);
    }

    public List<AccountEvent> getEventHistory(String accountId) {
        return eventStore.loadEvents(accountId, AccountEvent.class, COLLECTION);
    }

    private void persistAndSnapshot(BankAccount account) {
        eventStore.appendAll(account.getUncommittedEvents().stream()
                .map(e -> (AccountEvent) e).toList(), COLLECTION);
        account.clearUncommittedEvents();

        long eventCount = eventStore.countEvents(account.getAccountId(), COLLECTION);
        if (eventCount >= SNAPSHOT_THRESHOLD && eventCount % SNAPSHOT_THRESHOLD == 0) {
            var snapshotDoc = new SnapshotDocument(
                    UUID.randomUUID().toString(),
                    account.getAccountId(),
                    AGGREGATE_TYPE,
                    account.getVersion(),
                    Instant.now(),
                    account.toSnapshot());
            eventStore.saveSnapshot(snapshotDoc);
        }
    }
}
