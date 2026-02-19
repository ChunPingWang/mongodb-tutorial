package com.mongodb.course.m04.java23;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FinancialProductService {

    private static final String COLLECTION = "financial_products";

    private final MongoTemplate mongoTemplate;

    public FinancialProductService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public String describe(FinancialProduct product) {
        return switch (product) {
            case SavingsAccount sa -> "Savings: %s at %s%% interest".formatted(sa.name(), sa.interestRate());
            case FixedDeposit fd -> "Fixed Deposit: %s for %d months".formatted(fd.name(), fd.termMonths());
            case InsurancePolicy ip -> "Insurance: %s (%s)".formatted(ip.name(), ip.policyType());
        };
    }

    public <T extends FinancialProduct> T save(T product) {
        return mongoTemplate.save(product, COLLECTION);
    }

    public List<SavingsAccount> findAllSavings() {
        return findByType(SavingsAccount.class);
    }

    public List<FixedDeposit> findAllFixedDeposits() {
        return findByType(FixedDeposit.class);
    }

    public List<InsurancePolicy> findAllInsurancePolicies() {
        return findByType(InsurancePolicy.class);
    }

    private <T> List<T> findByType(Class<T> type) {
        Query query = Query.query(Criteria.where("_class").is(type.getName()));
        return mongoTemplate.find(query, type, COLLECTION);
    }
}
