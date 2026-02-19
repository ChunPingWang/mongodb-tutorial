package com.mongodb.course.m01.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB representation of an insurance policy.
 * New fields can be added freely — no schema migration required.
 */
@Document(collection = "insurance_policies")
public class InsurancePolicyDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String policyNumber;

    private String holderName;
    private String policyType;
    private BigDecimal premium;
    private LocalDate startDate;
    private LocalDate endDate;

    // Schema evolution: simply add new fields — existing documents still readable
    private List<String> additionalClauses = new ArrayList<>();

    // Another evolution: add nested structures without migration
    private RiskAssessment riskAssessment;

    public InsurancePolicyDocument() {}

    public InsurancePolicyDocument(String policyNumber, String holderName, String policyType,
                                    BigDecimal premium, LocalDate startDate, LocalDate endDate) {
        this.policyNumber = policyNumber;
        this.holderName = holderName;
        this.policyType = policyType;
        this.premium = premium;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getId() { return id; }
    public String getPolicyNumber() { return policyNumber; }
    public String getHolderName() { return holderName; }
    public String getPolicyType() { return policyType; }
    public BigDecimal getPremium() { return premium; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public List<String> getAdditionalClauses() { return additionalClauses; }
    public RiskAssessment getRiskAssessment() { return riskAssessment; }

    public void addClause(String clause) { additionalClauses.add(clause); }
    public void setRiskAssessment(RiskAssessment assessment) { this.riskAssessment = assessment; }

    public record RiskAssessment(String level, double score, String assessedBy) {}
}
