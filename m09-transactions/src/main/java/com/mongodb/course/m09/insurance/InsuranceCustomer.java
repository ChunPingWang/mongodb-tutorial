package com.mongodb.course.m09.insurance;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("m09_insurance_customers")
public class InsuranceCustomer {

    @Id
    private String id;

    private String customerNumber;

    private String name;

    private String email;

    private CustomerStatus status;

    private int policyCount;

    public InsuranceCustomer() {
    }

    public InsuranceCustomer(String customerNumber, String name, String email, CustomerStatus status) {
        this.customerNumber = customerNumber;
        this.name = name;
        this.email = email;
        this.status = status;
        this.policyCount = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public void setStatus(CustomerStatus status) {
        this.status = status;
    }

    public int getPolicyCount() {
        return policyCount;
    }

    public void setPolicyCount(int policyCount) {
        this.policyCount = policyCount;
    }
}
