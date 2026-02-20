package com.mongodb.course.m10.banking.domain.model;

import com.mongodb.course.m10.banking.domain.event.LoanApplicationSubmitted;
import com.mongodb.course.m10.banking.domain.event.LoanPreliminaryReviewPassed;
import com.mongodb.course.m10.banking.domain.event.LoanPreliminaryReviewRejected;
import com.mongodb.course.m10.banking.domain.specification.IncomeToPaymentRatioSpec;
import com.mongodb.course.m10.shared.DomainEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root — LoanApplication.
 * Pure domain class with ZERO Spring/MongoDB dependencies.
 */
public class LoanApplication {

    private String id;
    private Applicant applicant;
    private Money requestedAmount;
    private LoanTerm term;
    private LoanStatus status;
    private String reviewResult;
    private Instant createdAt;
    private Instant updatedAt;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private LoanApplication() {
    }

    // ── Business factory ────────────────────────────────────────────────
    public static LoanApplication submit(Applicant applicant, Money requestedAmount, LoanTerm term) {
        var app = new LoanApplication();
        app.applicant = applicant;
        app.requestedAmount = requestedAmount;
        app.term = term;
        app.status = LoanStatus.SUBMITTED;
        app.createdAt = Instant.now();
        app.updatedAt = app.createdAt;
        app.registerEvent(new LoanApplicationSubmitted(
                null, applicant.name(), requestedAmount, Instant.now()));
        return app;
    }

    // ── Persistence reconstitution — NO validation, NO events ───────────
    public static LoanApplication reconstitute(String id, Applicant applicant,
                                               Money requestedAmount, LoanTerm term,
                                               LoanStatus status, String reviewResult,
                                               Instant createdAt, Instant updatedAt) {
        var app = new LoanApplication();
        app.id = id;
        app.applicant = applicant;
        app.requestedAmount = requestedAmount;
        app.term = term;
        app.status = status;
        app.reviewResult = reviewResult;
        app.createdAt = createdAt;
        app.updatedAt = updatedAt;
        return app;
    }

    // ── Behavior ────────────────────────────────────────────────────────
    public void performPreliminaryReview(IncomeToPaymentRatioSpec spec) {
        if (status != LoanStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Can only perform preliminary review on SUBMITTED applications, current: " + status);
        }
        Money annualPayment = term.computeAnnualPayment(requestedAmount);
        if (spec.isSatisfiedBy(applicant.annualIncome(), annualPayment)) {
            status = LoanStatus.PRELIMINARY_PASSED;
            reviewResult = "Income meets requirement";
            registerEvent(new LoanPreliminaryReviewPassed(id, Instant.now()));
        } else {
            status = LoanStatus.PRELIMINARY_REJECTED;
            reviewResult = "Income does not meet requirement";
            registerEvent(new LoanPreliminaryReviewRejected(id, reviewResult, Instant.now()));
        }
        updatedAt = Instant.now();
    }

    // ── Domain events ───────────────────────────────────────────────────
    private void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    // ── Accessors ───────────────────────────────────────────────────────
    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public Applicant getApplicant() { return applicant; }

    public Money getRequestedAmount() { return requestedAmount; }

    public LoanTerm getTerm() { return term; }

    public LoanStatus getStatus() { return status; }

    public String getReviewResult() { return reviewResult; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
}
