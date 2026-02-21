package com.mongodb.course.m19.transfer.step;

import com.mongodb.course.m19.account.event.AccountEvent;
import com.mongodb.course.m19.account.event.FundsTransferredIn;
import com.mongodb.course.m19.account.model.BankAccount;
import com.mongodb.course.m19.infrastructure.EventStore;
import com.mongodb.course.m19.infrastructure.saga.SagaContext;
import com.mongodb.course.m19.infrastructure.saga.SagaStep;
import com.mongodb.course.m19.projection.AccountSummaryProjector;
import com.mongodb.course.m19.projection.TransactionLedgerProjector;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class CreditTargetAccountStep implements SagaStep {

    private static final String ACCOUNT_EVENTS = "m19_account_events";

    private final EventStore eventStore;
    private final AccountSummaryProjector summaryProjector;
    private final TransactionLedgerProjector ledgerProjector;

    public CreditTargetAccountStep(EventStore eventStore,
                                   AccountSummaryProjector summaryProjector,
                                   TransactionLedgerProjector ledgerProjector) {
        this.eventStore = eventStore;
        this.summaryProjector = summaryProjector;
        this.ledgerProjector = ledgerProjector;
    }

    @Override
    public String name() {
        return "CREDIT_TARGET";
    }

    @Override
    public void execute(SagaContext context) {
        String sourceAccountId = context.get("sourceAccountId", String.class);
        String targetAccountId = context.get("targetAccountId", String.class);
        BigDecimal amount = new BigDecimal(context.get("amount", String.class));

        var events = eventStore.loadEvents(targetAccountId, AccountEvent.class, ACCOUNT_EVENTS);
        var account = BankAccount.replayFrom(events);

        var transferIn = new FundsTransferredIn(
                UUID.randomUUID().toString(), targetAccountId,
                account.getVersion() + 1, Instant.now(),
                amount, sourceAccountId, "Transfer from " + sourceAccountId);
        account.applyTransferIn(transferIn);

        var uncommitted = List.copyOf(account.getUncommittedEvents());
        account.clearUncommittedEvents();
        eventStore.appendAll(uncommitted, ACCOUNT_EVENTS);

        for (var event : uncommitted) {
            summaryProjector.project(event);
            ledgerProjector.project(event);
        }
    }

    @Override
    public void compensate(SagaContext context) {
        String targetAccountId = context.get("targetAccountId", String.class);
        BigDecimal amount = new BigDecimal(context.get("amount", String.class));

        var events = eventStore.loadEvents(targetAccountId, AccountEvent.class, ACCOUNT_EVENTS);
        var account = BankAccount.replayFrom(events);

        account.withdraw(amount, "Reversal: transfer compensation");

        var uncommitted = List.copyOf(account.getUncommittedEvents());
        account.clearUncommittedEvents();
        eventStore.appendAll(uncommitted, ACCOUNT_EVENTS);

        for (var event : uncommitted) {
            summaryProjector.project(event);
            ledgerProjector.project(event);
        }
    }
}
