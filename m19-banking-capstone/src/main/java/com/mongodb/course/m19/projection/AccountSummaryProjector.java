package com.mongodb.course.m19.projection;

import com.mongodb.course.m19.account.event.*;
import com.mongodb.course.m19.projection.readmodel.AccountSummaryDocument;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AccountSummaryProjector {

    private static final String COLLECTION = "m19_account_summaries";

    private final MongoTemplate mongoTemplate;

    public AccountSummaryProjector(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void project(AccountEvent event) {
        switch (event) {
            case AccountOpened e -> {
                var doc = new AccountSummaryDocument(
                        e.aggregateId(),
                        e.accountHolder(),
                        e.initialBalance(),
                        e.currency(),
                        1, 0, 0, 0, 0,
                        BigDecimal.ZERO,
                        false,
                        e.occurredAt(),
                        e.version());
                mongoTemplate.insert(doc, COLLECTION);
            }
            case FundsDeposited e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var update = new Update()
                        .inc("currentBalance", new Decimal128(e.amount()))
                        .inc("totalTransactions", 1)
                        .inc("depositCount", 1)
                        .set("lastActivityAt", e.occurredAt())
                        .set("projectedVersion", e.version());
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
            case FundsWithdrawn e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var update = new Update()
                        .inc("currentBalance", new Decimal128(e.amount().negate()))
                        .inc("totalTransactions", 1)
                        .inc("withdrawalCount", 1)
                        .set("lastActivityAt", e.occurredAt())
                        .set("projectedVersion", e.version());
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
            case FundsTransferredOut e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var update = new Update()
                        .inc("currentBalance", new Decimal128(e.amount().negate()))
                        .inc("totalTransactions", 1)
                        .inc("transferOutCount", 1)
                        .set("lastActivityAt", e.occurredAt())
                        .set("projectedVersion", e.version());
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
            case FundsTransferredIn e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var update = new Update()
                        .inc("currentBalance", new Decimal128(e.amount()))
                        .inc("totalTransactions", 1)
                        .inc("transferInCount", 1)
                        .set("lastActivityAt", e.occurredAt())
                        .set("projectedVersion", e.version());
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
            case InterestAccrued e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var update = new Update()
                        .inc("currentBalance", new Decimal128(e.amount()))
                        .inc("totalTransactions", 1)
                        .inc("totalInterestEarned", new Decimal128(e.amount()))
                        .set("lastActivityAt", e.occurredAt())
                        .set("projectedVersion", e.version());
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
            case AccountClosed e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var update = new Update()
                        .set("closed", true)
                        .inc("totalTransactions", 1)
                        .set("lastActivityAt", e.occurredAt())
                        .set("projectedVersion", e.version());
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
        }
    }
}
