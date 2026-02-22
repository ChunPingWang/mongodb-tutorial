package com.mongodb.course.m08.insurance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Document("m08_insurance_policies")
public class InsurancePolicyDocument {

    @Id
    private String id;

    @NotBlank
    private String policyNumber;

    @NotBlank
    private String holderName;

    @NotNull
    private PolicyType policyType;

    @NotNull
    @Positive
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal premium;

    @NotNull
    @Positive
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal coverageAmount;

    @NotNull
    private PolicyStatus status;

    private LocalDate effectiveDate;
    private LocalDate expirationDate;

    public InsurancePolicyDocument() {
    }

    public InsurancePolicyDocument(String policyNumber, String holderName, PolicyType policyType,
                                   BigDecimal premium, BigDecimal coverageAmount,
                                   LocalDate effectiveDate, LocalDate expirationDate) {
        this.policyNumber = policyNumber;
        this.holderName = holderName;
        this.policyType = policyType;
        this.premium = premium;
        this.coverageAmount = coverageAmount;
        this.status = PolicyStatus.ACTIVE;
        this.effectiveDate = effectiveDate;
        this.expirationDate = expirationDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public String getHolderName() {
        return holderName;
    }

    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }

    public PolicyType getPolicyType() {
        return policyType;
    }

    public void setPolicyType(PolicyType policyType) {
        this.policyType = policyType;
    }

    public BigDecimal getPremium() {
        return premium;
    }

    public void setPremium(BigDecimal premium) {
        this.premium = premium;
    }

    public BigDecimal getCoverageAmount() {
        return coverageAmount;
    }

    public void setCoverageAmount(BigDecimal coverageAmount) {
        this.coverageAmount = coverageAmount;
    }

    public PolicyStatus getStatus() {
        return status;
    }

    public void setStatus(PolicyStatus status) {
        this.status = status;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }
}
