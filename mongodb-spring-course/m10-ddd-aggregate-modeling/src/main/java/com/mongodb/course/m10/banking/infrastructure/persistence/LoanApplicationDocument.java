package com.mongodb.course.m10.banking.infrastructure.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document("m10_loan_applications")
public class LoanApplicationDocument {

    @Id
    private String id;
    private String applicantName;
    private String applicantNationalId;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal applicantAnnualIncome;
    private String applicantAnnualIncomeCurrency;
    private String applicantEmployer;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal requestedAmount;
    private String requestedAmountCurrency;
    private int termYears;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal annualInterestRate;
    private String status;
    private String reviewResult;
    private Instant createdAt;
    private Instant updatedAt;
    private List<Map<String, Object>> domainEvents;

    public LoanApplicationDocument() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public String getApplicantNationalId() { return applicantNationalId; }
    public void setApplicantNationalId(String applicantNationalId) { this.applicantNationalId = applicantNationalId; }
    public BigDecimal getApplicantAnnualIncome() { return applicantAnnualIncome; }
    public void setApplicantAnnualIncome(BigDecimal applicantAnnualIncome) { this.applicantAnnualIncome = applicantAnnualIncome; }
    public String getApplicantAnnualIncomeCurrency() { return applicantAnnualIncomeCurrency; }
    public void setApplicantAnnualIncomeCurrency(String applicantAnnualIncomeCurrency) { this.applicantAnnualIncomeCurrency = applicantAnnualIncomeCurrency; }
    public String getApplicantEmployer() { return applicantEmployer; }
    public void setApplicantEmployer(String applicantEmployer) { this.applicantEmployer = applicantEmployer; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(BigDecimal requestedAmount) { this.requestedAmount = requestedAmount; }
    public String getRequestedAmountCurrency() { return requestedAmountCurrency; }
    public void setRequestedAmountCurrency(String requestedAmountCurrency) { this.requestedAmountCurrency = requestedAmountCurrency; }
    public int getTermYears() { return termYears; }
    public void setTermYears(int termYears) { this.termYears = termYears; }
    public BigDecimal getAnnualInterestRate() { return annualInterestRate; }
    public void setAnnualInterestRate(BigDecimal annualInterestRate) { this.annualInterestRate = annualInterestRate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReviewResult() { return reviewResult; }
    public void setReviewResult(String reviewResult) { this.reviewResult = reviewResult; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<Map<String, Object>> getDomainEvents() { return domainEvents; }
    public void setDomainEvents(List<Map<String, Object>> domainEvents) { this.domainEvents = domainEvents; }
}
