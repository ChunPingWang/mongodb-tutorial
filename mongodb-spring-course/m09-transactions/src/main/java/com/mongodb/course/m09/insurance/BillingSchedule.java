package com.mongodb.course.m09.insurance;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Document("m09_billing_schedules")
public class BillingSchedule {

    @Id
    private String id;

    private String policyNumber;

    private String customerNumber;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal monthlyAmount;

    private LocalDate startDate;

    private LocalDate endDate;

    private BillingStatus status;

    public BillingSchedule() {
    }

    public BillingSchedule(String policyNumber, String customerNumber, BigDecimal monthlyAmount,
                           LocalDate startDate, LocalDate endDate) {
        this.policyNumber = policyNumber;
        this.customerNumber = customerNumber;
        this.monthlyAmount = monthlyAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = BillingStatus.ACTIVE;
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

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public BigDecimal getMonthlyAmount() {
        return monthlyAmount;
    }

    public void setMonthlyAmount(BigDecimal monthlyAmount) {
        this.monthlyAmount = monthlyAmount;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BillingStatus getStatus() {
        return status;
    }

    public void setStatus(BillingStatus status) {
        this.status = status;
    }

    public enum BillingStatus {
        ACTIVE, CANCELLED
    }
}
