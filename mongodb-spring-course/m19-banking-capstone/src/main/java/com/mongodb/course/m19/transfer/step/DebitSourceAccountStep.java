package com.mongodb.course.m19.transfer.step;

import com.mongodb.course.m19.account.event.AccountEvent;
import com.mongodb.course.m19.account.event.FundsDeposited;
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
public class DebitSourceAccountStep implements SagaStep {

    private static final String ACCOUNT_EVENTS = "m19_account_events";

    private final EventStore eventStore;
    private final AccountSummaryProjector summaryProjector;
    private final TransactionLedgerProjector ledgerProjector;

    public DebitSourceAccountStep(EventStore eventStore,
                                  AccountSummaryProjector summaryProjector,
                                  TransactionLedgerProjector ledgerProjector) {
        this.eventStore = eventStore;
        this.summaryProjector = summaryProjector;
        this.ledgerProjector = ledgerProjector;
    }

    @Override
    public String name() {
        return "DEBIT_SOURCE";
    }

    @Override
    public void execute(SagaContext context) {
        String sourceAccountId = context.get("sourceAccountId", String.class);
        String targetAccountId = context.get("targetAccountId", String.class);
        BigDecimal amount = new BigDecimal(context.get("amount", String.class));

        var events = eventStore.loadEvents(sourceAccountId, AccountEvent.class, ACCOUNT_EVENTS);
        var account = BankAccount.replayFrom(events);

        account.transferOut(amount, targetAccountId, "Transfer to " + targetAccountId);

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
        String sourceAccountId = context.get("sourceAccountId", String.class);
        BigDecimal amount = new BigDecimal(context.get("amount", String.class));

        var events = eventStore.loadEvents(sourceAccountId, AccountEvent.class, ACCOUNT_EVENTS);
        var account = BankAccount.replayFrom(events);

        account.deposit(amount, "Reversal: transfer compensation");

        var uncommitted = List.copyOf(account.getUncommittedEvents());
        account.clearUncommittedEvents();
        eventStore.appendAll(uncommitted, ACCOUNT_EVENTS);

        for (var event : uncommitted) {
            summaryProjector.project(event);
            ledgerProjector.project(event);
        }
    }
}
