package com.mongodb.course.m01.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface InsurancePolicyMongoRepository extends MongoRepository<InsurancePolicyDocument, String> {

    Optional<InsurancePolicyDocument> findByPolicyNumber(String policyNumber);
}
