package com.mongodb.course.m10.banking.application;

import com.mongodb.course.m10.banking.domain.model.Applicant;
import com.mongodb.course.m10.banking.domain.model.LoanApplication;
import com.mongodb.course.m10.banking.domain.model.LoanTerm;
import com.mongodb.course.m10.banking.domain.model.Money;
import com.mongodb.course.m10.banking.domain.port.LoanApplicationRepository;
import com.mongodb.course.m10.banking.domain.specification.IncomeToPaymentRatioSpec;
import org.springframework.stereotype.Service;

@Service
public class LoanApplicationService {

    private static final int INCOME_RATIO = 5;

    private final LoanApplicationRepository repository;

    public LoanApplicationService(LoanApplicationRepository repository) {
        this.repository = repository;
    }

    public LoanApplication submitApplication(Applicant applicant, Money amount, LoanTerm term) {
        LoanApplication app = LoanApplication.submit(applicant, amount, term);
        return repository.save(app);
    }

    public LoanApplication performPreliminaryReview(String applicationId) {
        LoanApplication app = repository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        app.performPreliminaryReview(new IncomeToPaymentRatioSpec(INCOME_RATIO));
        return repository.save(app);
    }
}
