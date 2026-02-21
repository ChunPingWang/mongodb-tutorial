package com.mongodb.course.m19.loan.service;

import com.mongodb.course.m19.loan.infrastructure.LoanApplicationDocument;
import com.mongodb.course.m19.loan.infrastructure.LoanApplicationMapper;
import com.mongodb.course.m19.loan.model.Applicant;
import com.mongodb.course.m19.loan.model.LoanApplication;
import com.mongodb.course.m19.loan.specification.DebtToIncomeRatioSpec;
import com.mongodb.course.m19.loan.specification.MinimumBalanceSpec;
import com.mongodb.course.m19.projection.DashboardQueryService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class LoanApplicationService {

    private static final String COLLECTION = "m19_loan_applications";

    private final MongoTemplate mongoTemplate;
    private final DashboardQueryService dashboardQueryService;
    private final MinimumBalanceSpec minimumBalanceSpec = new MinimumBalanceSpec();
    private final DebtToIncomeRatioSpec debtToIncomeRatioSpec = new DebtToIncomeRatioSpec();

    public LoanApplicationService(MongoTemplate mongoTemplate, DashboardQueryService dashboardQueryService) {
        this.mongoTemplate = mongoTemplate;
        this.dashboardQueryService = dashboardQueryService;
    }

    public LoanApplication submit(Applicant applicant, BigDecimal requestedAmount, int termMonths) {
        var application = LoanApplication.submit(applicant, requestedAmount, termMonths);
        var doc = LoanApplicationMapper.toDocument(application);
        mongoTemplate.insert(doc, COLLECTION);
        return application;
    }

    public LoanApplication review(String applicationId) {
        var doc = mongoTemplate.findById(applicationId, LoanApplicationDocument.class, COLLECTION);
        if (doc == null) {
            throw new IllegalArgumentException("Loan application not found: " + applicationId);
        }

        var application = LoanApplicationMapper.toDomain(doc);
        BigDecimal currentBalance = dashboardQueryService.getAccountBalance(application.getApplicant().accountId());

        application.review(minimumBalanceSpec, debtToIncomeRatioSpec, currentBalance);

        var updatedDoc = LoanApplicationMapper.toDocument(application);
        mongoTemplate.save(updatedDoc, COLLECTION);
        return application;
    }
}
