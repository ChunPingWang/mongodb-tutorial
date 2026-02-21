package com.mongodb.course.m19.loan.model;

import com.mongodb.course.m19.loan.specification.DebtToIncomeRatioSpec;
import com.mongodb.course.m19.loan.specification.MinimumBalanceSpec;

import java.math.BigDecimal;
import java.util.UUID;

public class LoanApplication {

    private String id;
    private Applicant applicant;
    private BigDecimal requestedAmount;
    private int termMonths;
    private LoanStatus status;
    private String rejectionReason;

    private LoanApplication() {
    }

    public static LoanApplication submit(Applicant applicant, BigDecimal requestedAmount, int termMonths) {
        var app = new LoanApplication();
        app.id = UUID.randomUUID().toString();
        app.applicant = applicant;
        app.requestedAmount = requestedAmount;
        app.termMonths = termMonths;
        app.status = LoanStatus.SUBMITTED;
        return app;
    }

    public void review(MinimumBalanceSpec balanceSpec, DebtToIncomeRatioSpec dtiSpec,
                       BigDecimal currentBalance) {
        this.status = LoanStatus.UNDER_REVIEW;

        if (!balanceSpec.isSatisfiedBy(currentBalance, requestedAmount)) {
            this.status = LoanStatus.REJECTED;
            this.rejectionReason = "Insufficient account balance: requires at least 10% of loan amount";
            return;
        }

        if (!dtiSpec.isSatisfiedBy(applicant.annualIncome(), requestedAmount, termMonths)) {
            this.status = LoanStatus.REJECTED;
            this.rejectionReason = "Debt-to-income ratio too high: annual income insufficient for loan payments";
            return;
        }

        this.status = LoanStatus.APPROVED;
    }

    public String getId() { return id; }
    public Applicant getApplicant() { return applicant; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public int getTermMonths() { return termMonths; }
    public LoanStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }

    // For persistence reconstruction
    public static LoanApplication reconstitute(String id, Applicant applicant, BigDecimal requestedAmount,
                                               int termMonths, LoanStatus status, String rejectionReason) {
        var app = new LoanApplication();
        app.id = id;
        app.applicant = applicant;
        app.requestedAmount = requestedAmount;
        app.termMonths = termMonths;
        app.status = status;
        app.rejectionReason = rejectionReason;
        return app;
    }
}
