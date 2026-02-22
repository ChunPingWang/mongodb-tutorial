package com.mongodb.course.m05.insurance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface InsurancePolicyRepository extends MongoRepository<InsurancePolicyDocument, String> {

    List<InsurancePolicyDocument> findByPolicyType(PolicyType policyType);

    List<InsurancePolicyDocument> findByPremiumGreaterThan(BigDecimal premium);

    List<InsurancePolicyDocument> findByStatus(PolicyStatus status);

    Page<InsurancePolicyDocument> findByPolicyType(PolicyType policyType, Pageable pageable);

    @Query("{ 'policyType': { $in: ?0 }, 'status': 'ACTIVE' }")
    List<InsurancePolicyDocument> findActivePoliciesByTypes(List<PolicyType> types);
}
