package com.mongodb.course.m11.insurance.service;

import com.mongodb.course.m11.insurance.model.Policy;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PolicyService {

    private static final String COLLECTION = "m11_policies";

    private final MongoTemplate mongoTemplate;

    public PolicyService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public <T extends Policy> T save(T policy) {
        return mongoTemplate.save(policy, COLLECTION);
    }

    public List<Policy> findAll() {
        return mongoTemplate.findAll(Policy.class, COLLECTION);
    }

    public <T extends Policy> List<T> findByType(Class<T> type) {
        TypeAlias alias = type.getAnnotation(TypeAlias.class);
        String classValue = alias != null ? alias.value() : type.getName();
        Query query = Query.query(Criteria.where("_class").is(classValue));
        return mongoTemplate.find(query, type, COLLECTION);
    }

    public Policy findByPolicyNumber(String policyNumber) {
        Query query = Query.query(Criteria.where("policyNumber").is(policyNumber));
        return mongoTemplate.findOne(query, Policy.class, COLLECTION);
    }

    public List<Policy> findByHolderName(String holderName) {
        Query query = Query.query(Criteria.where("holderName").is(holderName));
        return mongoTemplate.find(query, Policy.class, COLLECTION);
    }
}
