package com.mongodb.course.m13.banking.projection;

import com.mongodb.course.m13.banking.event.*;
import com.mongodb.course.m13.banking.readmodel.TransactionHistoryDocument;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionHistoryProjector {

    private static final String COLLECTION = "m13_transaction_history";

    private final MongoTemplate mongoTemplate;

    public TransactionHistoryProjector(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void project(AccountEvent event) {
        switch (event) {
            case AccountOpened e -> {
                var doc = new TransactionHistoryDocument(
                        e.eventId(),
                        e.aggregateId(),
                        "OPENING",
                        e.initialBalance(),
                        "Account opened",
                        null,
                        e.initialBalance(),
                        e.occurredAt());
                mongoTemplate.insert(doc, COLLECTION);
            }
            case FundsDeposited e -> {
                var balanceAfter = getLatestBalance(e.aggregateId()).add(e.amount());
                var doc = new TransactionHistoryDocument(
                        e.eventId(),
                        e.aggregateId(),
                        "DEPOSIT",
                        e.amount(),
                        e.description(),
                        null,
                        balanceAfter,
                        e.occurredAt());
                mongoTemplate.insert(doc, COLLECTION);
            }
            case FundsWithdrawn e -> {
                var balanceAfter = getLatestBalance(e.aggregateId()).subtract(e.amount());
                var doc = new TransactionHistoryDocument(
                        e.eventId(),
                        e.aggregateId(),
                        "WITHDRAWAL",
                        e.amount(),
                        e.description(),
                        null,
                        balanceAfter,
                        e.occurredAt());
                mongoTemplate.insert(doc, COLLECTION);
            }
            case FundsTransferred e -> {
                var balanceAfter = getLatestBalance(e.aggregateId()).subtract(e.amount());
                var doc = new TransactionHistoryDocument(
                        e.eventId(),
                        e.aggregateId(),
                        "TRANSFER",
                        e.amount(),
                        e.description(),
                        e.targetAccountId(),
                        balanceAfter,
                        e.occurredAt());
                mongoTemplate.insert(doc, COLLECTION);
            }
        }
    }

    public void rebuildAll(List<AccountEvent> events) {
        mongoTemplate.remove(new Query(), COLLECTION);
        events.forEach(this::project);
    }

    private BigDecimal getLatestBalance(String accountId) {
        var query = Query.query(Criteria.where("accountId").is(accountId))
                .with(Sort.by(Sort.Direction.DESC, "occurredAt"))
                .limit(1);
        var latest = mongoTemplate.findOne(query, TransactionHistoryDocument.class, COLLECTION);
        return latest != null ? latest.balanceAfter() : BigDecimal.ZERO;
    }
}
