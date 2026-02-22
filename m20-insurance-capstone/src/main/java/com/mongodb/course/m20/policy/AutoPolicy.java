package com.mongodb.course.m20.policy;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

@Document("m20_policies")
@TypeAlias("AutoPolicy")
public final class AutoPolicy implements Policy {

    @Id
    private String id;
    private String policyNumber;
    private String holderName;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal basePremium;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal coverageAmount;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalClaimsPaid;
    private String vehicleType;
    private int accidentCount;

    public AutoPolicy() {
    }

    public AutoPolicy(String id, String policyNumber, String holderName,
                      BigDecimal basePremium, BigDecimal coverageAmount,
                      String vehicleType) {
        this.id = id;
        this.policyNumber = policyNumber;
        this.holderName = holderName;
        this.basePremium = basePremium;
        this.coverageAmount = coverageAmount;
        this.totalClaimsPaid = BigDecimal.ZERO;
        this.vehicleType = vehicleType;
        this.accidentCount = 0;
    }

    @Override public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @Override public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }

    @Override public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }

    @Override public BigDecimal getBasePremium() { return basePremium; }
    public void setBasePremium(BigDecimal basePremium) { this.basePremium = basePremium; }

    @Override public BigDecimal getCoverageAmount() { return coverageAmount; }
    public void setCoverageAmount(BigDecimal coverageAmount) { this.coverageAmount = coverageAmount; }

    @Override public BigDecimal getTotalClaimsPaid() { return totalClaimsPaid; }
    public void setTotalClaimsPaid(BigDecimal totalClaimsPaid) { this.totalClaimsPaid = totalClaimsPaid; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public int getAccidentCount() { return accidentCount; }
    public void setAccidentCount(int accidentCount) { this.accidentCount = accidentCount; }
}
