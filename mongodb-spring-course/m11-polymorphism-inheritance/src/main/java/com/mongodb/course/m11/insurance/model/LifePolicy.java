package com.mongodb.course.m11.insurance.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

@Document("m11_policies")
@TypeAlias("life")
public final class LifePolicy implements Policy {

    @Id
    private String id;
    private String policyNumber;
    private String holderName;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal basePremium;
    private int insuredAge;
    private int termYears;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal sumAssured;

    public LifePolicy() {
    }

    public LifePolicy(String id, String policyNumber, String holderName,
                      BigDecimal basePremium, int insuredAge, int termYears, BigDecimal sumAssured) {
        this.id = id;
        this.policyNumber = policyNumber;
        this.holderName = holderName;
        this.basePremium = basePremium;
        this.insuredAge = insuredAge;
        this.termYears = termYears;
        this.sumAssured = sumAssured;
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

    public int getInsuredAge() { return insuredAge; }
    public void setInsuredAge(int insuredAge) { this.insuredAge = insuredAge; }

    public int getTermYears() { return termYears; }
    public void setTermYears(int termYears) { this.termYears = termYears; }

    public BigDecimal getSumAssured() { return sumAssured; }
    public void setSumAssured(BigDecimal sumAssured) { this.sumAssured = sumAssured; }
}
