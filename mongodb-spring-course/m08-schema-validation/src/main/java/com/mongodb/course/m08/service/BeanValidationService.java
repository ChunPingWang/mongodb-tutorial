package com.mongodb.course.m08.service;

import com.mongodb.course.m08.banking.BankAccount;
import com.mongodb.course.m08.ecommerce.Product;
import com.mongodb.course.m08.insurance.InsurancePolicyDocument;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
public class BeanValidationService {

    private final MongoTemplate mongoTemplate;

    public BeanValidationService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public BankAccount saveBankAccount(BankAccount account) {
        return mongoTemplate.save(account);
    }

    public InsurancePolicyDocument savePolicy(InsurancePolicyDocument policy) {
        return mongoTemplate.save(policy);
    }

    public Product saveProduct(Product product) {
        return mongoTemplate.save(product);
    }
}
