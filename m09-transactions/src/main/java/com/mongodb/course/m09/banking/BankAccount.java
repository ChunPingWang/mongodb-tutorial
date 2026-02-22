package com.mongodb.course.m09.banking;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document("m09_bank_accounts")
public class BankAccount {

    @Id
    private String id;

    private String accountNumber;

    private String holderName;

    private AccountType type;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal balance;

    private AccountStatus status;

    private Instant openedAt;

    private Instant closedAt;

    public BankAccount() {
    }

    public BankAccount(String accountNumber, String holderName, AccountType type, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.type = type;
        this.balance = balance;
        this.status = AccountStatus.ACTIVE;
        this.openedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getHolderName() {
        return holderName;
    }

    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }
}
