package com.mongodb.course.m10.insurance.application;

import com.mongodb.course.m10.banking.domain.model.Money;
import com.mongodb.course.m10.insurance.domain.model.Claim;
import com.mongodb.course.m10.insurance.domain.model.ClaimItem;
import com.mongodb.course.m10.insurance.domain.model.ClaimantReference;
import com.mongodb.course.m10.insurance.domain.model.PolicyReference;
import com.mongodb.course.m10.insurance.domain.port.ClaimRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClaimService {

    private final ClaimRepository repository;

    public ClaimService(ClaimRepository repository) {
        this.repository = repository;
    }

    public Claim fileClaim(PolicyReference policyRef, ClaimantReference claimantRef,
                           List<ClaimItem> items, Money policyCoverage, Money deductible) {
        Claim claim = Claim.file(policyRef, claimantRef, items, policyCoverage, deductible);
        return repository.save(claim);
    }

    public Claim assessClaim(String claimId, String assessorName, Money approvedAmount, String notes) {
        Claim claim = repository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));
        claim.assess(assessorName, approvedAmount, notes);
        return repository.save(claim);
    }
}
