package com.mongodb.course.m20.policy;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PolicyService {

    private static final String COLLECTION = "m20_policies";

    private final MongoTemplate mongoTemplate;

    public PolicyService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Policy save(Policy policy) {
        return mongoTemplate.save(policy, COLLECTION);
    }

    public Optional<Policy> findById(String id) {
        return Optional.ofNullable(mongoTemplate.findById(id, Policy.class, COLLECTION));
    }

    public Optional<Policy> findByPolicyNumber(String policyNumber) {
        var query = Query.query(Criteria.where("policyNumber").is(policyNumber));
        return Optional.ofNullable(mongoTemplate.findOne(query, Policy.class, COLLECTION));
    }

    public <T extends Policy> List<T> findByType(Class<T> type) {
        var alias = type.getAnnotation(org.springframework.data.annotation.TypeAlias.class);
        var query = Query.query(Criteria.where("_class").is(alias.value()));
        return mongoTemplate.find(query, type, COLLECTION);
    }
}
