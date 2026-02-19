package com.mongodb.course.m05.banking;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.mongodb.client.result.UpdateResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class BankAccountService {

    private final BankAccountRepository repository;
    private final MongoTemplate mongoTemplate;

    public BankAccountService(BankAccountRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    // --- Repository-based methods ---

    public BankAccount createAccount(BankAccount account) {
        return repository.save(account);
    }

    public Optional<BankAccount> findById(String id) {
        return repository.findById(id);
    }

    public List<BankAccount> findByHolder(String holderName) {
        return repository.findByHolderName(holderName);
    }

    public BankAccount closeAccount(String id) {
        BankAccount account = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
        account.setStatus(AccountStatus.CLOSED);
        account.setClosedAt(Instant.now());
        return repository.save(account);
    }

    // --- MongoTemplate-based methods ---

    public UpdateResult deposit(String id, BigDecimal amount) {
        Query query = Query.query(Criteria.where("id").is(id));
        Update update = new Update().inc("balance", amount);
        return mongoTemplate.updateFirst(query, update, BankAccount.class);
    }

    public UpdateResult withdraw(String id, BigDecimal amount) {
        Query query = Query.query(Criteria.where("id").is(id));
        Update update = new Update().inc("balance", amount.negate());
        return mongoTemplate.updateFirst(query, update, BankAccount.class);
    }

    public UpdateResult freezeAccount(String id) {
        Query query = Query.query(Criteria.where("id").is(id));
        Update update = new Update().set("status", AccountStatus.FROZEN);
        return mongoTemplate.updateFirst(query, update, BankAccount.class);
    }

    public BankAccount findAndCloseAccount(String id) {
        Query query = Query.query(Criteria.where("id").is(id));
        Update update = new Update()
                .set("status", AccountStatus.CLOSED)
                .set("closedAt", Instant.now());
        return mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(false),
                BankAccount.class);
    }

    public UpdateResult addInterestToAll(AccountType type, BigDecimal interestAmount) {
        Query query = Query.query(Criteria.where("type").is(type));
        Update update = new Update().inc("balance", interestAmount);
        return mongoTemplate.updateMulti(query, update, BankAccount.class);
    }
}
