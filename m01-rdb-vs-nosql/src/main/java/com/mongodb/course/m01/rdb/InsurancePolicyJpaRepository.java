package com.mongodb.course.m01.rdb;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InsurancePolicyJpaRepository extends JpaRepository<InsurancePolicyEntity, Long> {

    Optional<InsurancePolicyEntity> findByPolicyNumber(String policyNumber);
}
