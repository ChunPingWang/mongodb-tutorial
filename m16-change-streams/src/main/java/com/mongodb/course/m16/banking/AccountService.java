package com.mongodb.course.m16.banking;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AccountService {

    private final MongoTemplate mongoTemplate;

    public AccountService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Account create(String accountHolder, long balance) {
        return mongoTemplate.insert(Account.of(accountHolder, balance));
    }

    public Account updateBalance(String id, long newBalance) {
        var query = Query.query(Criteria.where("_id").is(id));
        var update = new Update().set("balance", newBalance).set("updatedAt", Instant.now());
        mongoTemplate.updateFirst(query, update, Account.class);
        return mongoTemplate.findById(id, Account.class);
    }

    public void delete(String id) {
        var query = Query.query(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, Account.class);
    }
}
