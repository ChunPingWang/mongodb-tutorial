package com.mongodb.course.m10.banking.domain.port;

import com.mongodb.course.m10.banking.domain.model.LoanApplication;
import com.mongodb.course.m10.banking.domain.model.LoanStatus;

import java.util.List;
import java.util.Optional;

/**
 * Domain port — NO Spring Data interfaces.
 * Implemented by infrastructure adapter.
 */
public interface LoanApplicationRepository {
    LoanApplication save(LoanApplication application);
    Optional<LoanApplication> findById(String id);
    List<LoanApplication> findByStatus(LoanStatus status);
    List<LoanApplication> findByApplicantName(String name);
}
