package com.mongodb.course.m06.insurance;

import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class InsurancePolicyQueryService {

    private final MongoTemplate mongoTemplate;

    public InsurancePolicyQueryService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    // in()
    public List<InsurancePolicyDocument> findByPolicyTypes(List<PolicyType> types) {
        Query query = new Query(Criteria.where("policyType").in(types));
        return mongoTemplate.find(query, InsurancePolicyDocument.class);
    }

    // nin()
    public List<InsurancePolicyDocument> findExcludingTypes(List<PolicyType> excluded) {
        Query query = new Query(Criteria.where("policyType").nin(excluded));
        return mongoTemplate.find(query, InsurancePolicyDocument.class);
    }

    // Range on DECIMAL128 — explicit Decimal128 conversion for range operators
    public List<InsurancePolicyDocument> findByPremiumRange(BigDecimal min, BigDecimal max) {
        Query query = new Query(
                Criteria.where("premium").gte(new Decimal128(min)).lte(new Decimal128(max))
        );
        return mongoTemplate.find(query, InsurancePolicyDocument.class);
    }

    // lte() on date
    public List<InsurancePolicyDocument> findExpiringSoon(LocalDate before) {
        Query query = new Query(
                Criteria.where("expirationDate").lte(before)
                        .and("status").is(PolicyStatus.ACTIVE)
        );
        return mongoTemplate.find(query, InsurancePolicyDocument.class);
    }

    // Complex AND
    public List<InsurancePolicyDocument> findActiveHighCoverage(BigDecimal minCoverage) {
        Query query = new Query(
                new Criteria().andOperator(
                        Criteria.where("status").is(PolicyStatus.ACTIVE),
                        Criteria.where("coverageAmount").gte(new Decimal128(minCoverage))
                )
        );
        return mongoTemplate.find(query, InsurancePolicyDocument.class);
    }

    // Dynamic conditional query building
    public List<InsurancePolicyDocument> findByMultipleConditions(PolicyType type, PolicyStatus status,
                                                                  BigDecimal minPremium) {
        Criteria criteria = new Criteria();
        if (type != null) {
            criteria = criteria.and("policyType").is(type);
        }
        if (status != null) {
            criteria = criteria.and("status").is(status);
        }
        if (minPremium != null) {
            criteria = criteria.and("premium").gte(new Decimal128(minPremium));
        }
        return mongoTemplate.find(new Query(criteria), InsurancePolicyDocument.class);
    }

    // gt() on date
    public List<InsurancePolicyDocument> findNotExpired(LocalDate today) {
        Query query = new Query(Criteria.where("expirationDate").gt(today));
        return mongoTemplate.find(query, InsurancePolicyDocument.class);
    }

    // mongoTemplate.exists()
    public boolean existsByPolicyNumber(String policyNumber) {
        Query query = new Query(Criteria.where("policyNumber").is(policyNumber));
        return mongoTemplate.exists(query, InsurancePolicyDocument.class);
    }
}
