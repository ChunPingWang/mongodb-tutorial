package com.mongodb.course.m15.banking;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionQueryService {

    private static final String COLLECTION = "m15_transactions";

    private final MongoTemplate mongoTemplate;

    public TransactionQueryService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<Transaction> findByAccountAndDateRange(String accountId, Instant from, Instant to) {
        var query = Query.query(
                Criteria.where("accountId").is(accountId)
                        .and("transactionDate").gte(from).lte(to)
        );
        return mongoTemplate.find(query, Transaction.class, COLLECTION);
    }

    public List<Transaction> findByAccountTypeAndDateRange(String accountId, TransactionType type,
                                                            Instant from, Instant to) {
        var query = Query.query(
                Criteria.where("accountId").is(accountId)
                        .and("type").is(type)
                        .and("transactionDate").gte(from).lte(to)
        );
        return mongoTemplate.find(query, Transaction.class, COLLECTION);
    }

    public List<Transaction> findRecentByAccount(String accountId, int limit) {
        var query = Query.query(Criteria.where("accountId").is(accountId))
                .with(Sort.by(Sort.Direction.DESC, "transactionDate"))
                .limit(limit);
        return mongoTemplate.find(query, Transaction.class, COLLECTION);
    }

    public List<Document> findAccountAmountOnly(String accountId) {
        var filter = new Document("accountId", accountId);
        var projection = new Document("accountId", 1)
                .append("amount", 1)
                .append("_id", 0);

        var results = new ArrayList<Document>();
        mongoTemplate.getCollection(COLLECTION)
                .find(filter)
                .projection(projection)
                .into(results);
        return results;
    }

    public long countByAccount(String accountId) {
        var query = Query.query(Criteria.where("accountId").is(accountId));
        return mongoTemplate.count(query, COLLECTION);
    }
}
