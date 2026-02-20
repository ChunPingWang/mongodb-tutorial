package com.mongodb.course.m10.insurance.domain.port;

import com.mongodb.course.m10.insurance.domain.model.Claim;

import java.util.Optional;

public interface ClaimRepository {
    Claim save(Claim claim);
    Optional<Claim> findById(String id);
}
