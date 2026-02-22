package com.mongodb.course.m08.banking;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document("m08_bank_accounts")
public class BankAccount {

    @Id
    private String id;

    @NotBlank
    @Size(min = 5, max = 20)
    private String accountNumber;

    @NotBlank
    private String holderName;

    @NotNull
    private AccountType type;

    @NotNull
    @DecimalMin("0")
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal balance;

    @NotNull
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
