package com.mongodb.course.m19.projection;

import com.mongodb.course.m19.account.event.*;
import com.mongodb.course.m19.projection.readmodel.TransactionLedgerDocument;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransactionLedgerProjector {

    private static final String COLLECTION = "m19_transaction_ledger";

    private final MongoTemplate mongoTemplate;

    public TransactionLedgerProjector(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void project(AccountEvent event) {
        switch (event) {
            case AccountOpened e -> {
                var doc = new TransactionLedgerDocument(
                        e.eventId(), e.aggregateId(), "OPENING",
                        e.initialBalance(), "Account opened", null,
                        e.initialBalance(), e.occurredAt());
                mongoTemplate.insert(doc, COLLECTION);
            }
            case FundsDeposited e -> {
                var balanceAfter = getLatestBalance(e.aggregateId()).add(e.amount());
                var doc = new TransactionLedgerDocument(
                        e.eventId(), e.aggregateId(), "DEPOSIT",
                        e.amount(), e.description(), null,
                        balanceAfter, e.occurredAt());
                mongoTemplate.insert(doc, COLLECTION);
            }
            case FundsWithdrawn e -> {
                var balanceAfter = getLatestBalance(e.aggregateId()).subtract(e.amount());
                var doc = new TransactionLedgerDocument(
                        e.eventId(), e.aggregateId(), "WITHDRAWAL",
                        e.amount(), e.description(), null,
                        balanceAfter, e.occurredAt());
                mongoTemplate.insert(doc, COLLECTION);
            }
            case FundsTransferredOut e -> {
                var balanceAfter = getLatestBalance(e.aggregateId()).subtract(e.amount());
                var doc = new TransactionLedgerDocument(
                        e.eventId(), e.aggregateId(), "TRANSFER_OUT",
                        e.amount(), e.description(), e.targetAccountId(),
                        balanceAfter, e.occurredAt());
                mongoTemplate.insert(doc, COLLECTION);
            }
            case FundsTransferredIn e -> {
                var balanceAfter = getLatestBalance(e.aggregateId()).add(e.amount());
                var doc = new TransactionLedgerDocument(
                        e.eventId(), e.aggregateId(), "TRANSFER_IN",
                        e.amount(), e.description(), e.sourceAccountId(),
                        balanceAfter, e.occurredAt());
                mongoTemplate.insert(doc, COLLECTION);
            }
            case InterestAccrued e -> {
                var balanceAfter = getLatestBalance(e.aggregateId()).add(e.amount());
                var doc = new TransactionLedgerDocument(
                        e.eventId(), e.aggregateId(), "INTEREST",
                        e.amount(), "Interest accrued", null,
                        balanceAfter, e.occurredAt());
                mongoTemplate.insert(doc, COLLECTION);
            }
            case AccountClosed e -> {
                var doc = new TransactionLedgerDocument(
                        e.eventId(), e.aggregateId(), "CLOSING",
                        BigDecimal.ZERO, "Account closed", null,
                        BigDecimal.ZERO, e.occurredAt());
                mongoTemplate.insert(doc, COLLECTION);
            }
        }
    }

    private BigDecimal getLatestBalance(String accountId) {
        var query = Query.query(Criteria.where("accountId").is(accountId))
                .with(Sort.by(Sort.Direction.DESC, "occurredAt"))
                .limit(1);
        var latest = mongoTemplate.findOne(query, TransactionLedgerDocument.class, COLLECTION);
        return latest != null ? latest.balanceAfter() : BigDecimal.ZERO;
    }
}
