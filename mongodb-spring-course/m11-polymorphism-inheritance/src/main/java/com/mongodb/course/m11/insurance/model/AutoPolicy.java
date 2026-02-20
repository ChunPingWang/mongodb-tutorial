package com.mongodb.course.m11.insurance.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

@Document("m11_policies")
@TypeAlias("auto")
public final class AutoPolicy implements Policy {

    @Id
    private String id;
    private String policyNumber;
    private String holderName;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal basePremium;
    private String vehicleType;
    private int driverAge;

    public AutoPolicy() {
    }

    public AutoPolicy(String id, String policyNumber, String holderName,
                      BigDecimal basePremium, String vehicleType, int driverAge) {
        this.id = id;
        this.policyNumber = policyNumber;
        this.holderName = holderName;
        this.basePremium = basePremium;
        this.vehicleType = vehicleType;
        this.driverAge = driverAge;
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

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public int getDriverAge() { return driverAge; }
    public void setDriverAge(int driverAge) { this.driverAge = driverAge; }
}
