package com.mongodb.course.m17.banking;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    private final MongoTemplate mongoTemplate;

    public TransactionService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Transaction create(String accountId, long amount, String type) {
        return mongoTemplate.save(Transaction.of(accountId, amount, type));
    }

    public List<Transaction> findByAccountId(String accountId) {
        var query = Query.query(Criteria.where("accountId").is(accountId));
        return mongoTemplate.find(query, Transaction.class);
    }

    public List<Transaction> findAll() {
        return mongoTemplate.findAll(Transaction.class);
    }
}
