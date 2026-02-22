package com.mongodb.course.m04.referenced;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

@Document("accounts_ref")
public class AccountRef {

    @Id
    private String id;
    private String customerId;
    private String accountNumber;
    private String type;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal balance;

    public AccountRef() {
    }

    public AccountRef(String customerId, String accountNumber, String type, BigDecimal balance) {
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.type = type;
        this.balance = balance;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
