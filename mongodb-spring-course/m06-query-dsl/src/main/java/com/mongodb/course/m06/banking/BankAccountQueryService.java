package com.mongodb.course.m06.banking;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BankAccountQueryService {

    private final MongoTemplate mongoTemplate;

    public BankAccountQueryService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    // Criteria.where().is()
    public List<BankAccount> findByStatus(AccountStatus status) {
        Query query = new Query(Criteria.where("status").is(status));
        return mongoTemplate.find(query, BankAccount.class);
    }

    // Chained .and()
    public List<BankAccount> findByTypeAndStatus(AccountType type, AccountStatus status) {
        Query query = new Query(
                Criteria.where("type").is(type)
                        .and("status").is(status)
        );
        return mongoTemplate.find(query, BankAccount.class);
    }

    // orOperator()
    public List<BankAccount> findByTypeOrStatus(AccountType type, AccountStatus status) {
        Query query = new Query(
                new Criteria().orOperator(
                        Criteria.where("type").is(type),
                        Criteria.where("status").is(status)
                )
        );
        return mongoTemplate.find(query, BankAccount.class);
    }

    // gte() + lte() range — Decimal128 fields require explicit conversion
    public List<BankAccount> findByBalanceRange(BigDecimal min, BigDecimal max) {
        Query query = new Query(
                Criteria.where("balance").gte(new Decimal128(min)).lte(new Decimal128(max))
        );
        return mongoTemplate.find(query, BankAccount.class);
    }

    // andOperator() combining multiple Criteria
    public List<BankAccount> findHighValueActiveAccounts(BigDecimal minBalance) {
        Query query = new Query(
                new Criteria().andOperator(
                        Criteria.where("status").is(AccountStatus.ACTIVE),
                        Criteria.where("balance").gte(new Decimal128(minBalance))
                )
        );
        return mongoTemplate.find(query, BankAccount.class);
    }

    // regex()
    public List<BankAccount> findByHolderNameRegex(String pattern) {
        Query query = new Query(Criteria.where("holderName").regex(pattern));
        return mongoTemplate.find(query, BankAccount.class);
    }

    // regex("^prefix")
    public List<BankAccount> findByHolderNameStartingWith(String prefix) {
        Query query = new Query(Criteria.where("holderName").regex("^" + prefix));
        return mongoTemplate.find(query, BankAccount.class);
    }

    // Query.with(Sort)
    public List<BankAccount> findActiveAccountsSorted(String sortField, boolean ascending) {
        Query query = new Query(Criteria.where("status").is(AccountStatus.ACTIVE));
        Sort sort = ascending ? Sort.by(Sort.Direction.ASC, sortField)
                : Sort.by(Sort.Direction.DESC, sortField);
        query.with(sort);
        return mongoTemplate.find(query, BankAccount.class);
    }

    // Query.with(PageRequest) + count
    public List<BankAccount> findActiveAccountsPaged(int page, int size) {
        Query query = new Query(Criteria.where("status").is(AccountStatus.ACTIVE));
        query.with(PageRequest.of(page, size));
        return mongoTemplate.find(query, BankAccount.class);
    }

    // mongoTemplate.count()
    public long countByStatusAndType(AccountStatus status, AccountType type) {
        Query query = new Query(
                Criteria.where("status").is(status)
                        .and("type").is(type)
        );
        return mongoTemplate.count(query, BankAccount.class);
    }

    // query.fields().include().exclude("id")
    public List<BankAccount> findAccountNumbersOnly() {
        Query query = new Query();
        query.fields().include("accountNumber").exclude("id");
        return mongoTemplate.find(query, BankAccount.class);
    }

    // mongoTemplate.findDistinct()
    public List<String> findDistinctHolderNames() {
        return mongoTemplate.findDistinct(new Query(), "holderName", BankAccount.class, String.class);
    }
}
