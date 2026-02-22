package com.mongodb.course.m13.banking.projection;

import com.mongodb.course.m13.banking.event.*;
import com.mongodb.course.m13.banking.readmodel.AccountSummaryDocument;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountSummaryProjector {

    private static final String COLLECTION = "m13_account_summaries";

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
                        1, 0, 0, 0,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        e.occurredAt(),
                        e.version());
                mongoTemplate.insert(doc, COLLECTION);
            }
            case FundsDeposited e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var update = new Update()
                        .inc("currentBalance", new Decimal128(e.amount()))
                        .inc("totalTransactionCount", 1)
                        .inc("depositCount", 1)
                        .inc("totalDeposited", new Decimal128(e.amount()))
                        .set("lastActivityAt", e.occurredAt())
                        .set("projectedVersion", e.version());
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
            case FundsWithdrawn e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var update = new Update()
                        .inc("currentBalance", new Decimal128(e.amount().negate()))
                        .inc("totalTransactionCount", 1)
                        .inc("withdrawalCount", 1)
                        .inc("totalWithdrawn", new Decimal128(e.amount()))
                        .set("lastActivityAt", e.occurredAt())
                        .set("projectedVersion", e.version());
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
            case FundsTransferred e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var update = new Update()
                        .inc("currentBalance", new Decimal128(e.amount().negate()))
                        .inc("totalTransactionCount", 1)
                        .inc("transferCount", 1)
                        .inc("totalWithdrawn", new Decimal128(e.amount()))
                        .set("lastActivityAt", e.occurredAt())
                        .set("projectedVersion", e.version());
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
        }
    }

    public void rebuildAll(List<AccountEvent> events) {
        mongoTemplate.remove(new Query(), COLLECTION);
        events.forEach(this::project);
    }
}
