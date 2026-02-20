package com.mongodb.course.m11.insurance.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

@Document("m11_policies")
@TypeAlias("health")
public final class HealthPolicy implements Policy {

    @Id
    private String id;
    private String policyNumber;
    private String holderName;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal basePremium;
    private boolean hasDentalCoverage;
    private boolean hasVisionCoverage;
    private int deductible;

    public HealthPolicy() {
    }

    public HealthPolicy(String id, String policyNumber, String holderName,
                        BigDecimal basePremium, boolean hasDentalCoverage,
                        boolean hasVisionCoverage, int deductible) {
        this.id = id;
        this.policyNumber = policyNumber;
        this.holderName = holderName;
        this.basePremium = basePremium;
        this.hasDentalCoverage = hasDentalCoverage;
        this.hasVisionCoverage = hasVisionCoverage;
        this.deductible = deductible;
    }

    @Override
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @Override
    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }

    @Override
    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }

    @Override
    public BigDecimal getBasePremium() { return basePremium; }
    public void setBasePremium(BigDecimal basePremium) { this.basePremium = basePremium; }

    public boolean isHasDentalCoverage() { return hasDentalCoverage; }
    public void setHasDentalCoverage(boolean hasDentalCoverage) { this.hasDentalCoverage = hasDentalCoverage; }

    public boolean isHasVisionCoverage() { return hasVisionCoverage; }
    public void setHasVisionCoverage(boolean hasVisionCoverage) { this.hasVisionCoverage = hasVisionCoverage; }

    public int getDeductible() { return deductible; }
    public void setDeductible(int deductible) { this.deductible = deductible; }
}
