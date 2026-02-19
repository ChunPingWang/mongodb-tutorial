package com.mongodb.course.m04.embedded;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document("bank_customers_embedded")
public class BankCustomerDocument {

    @Id
    private String id;
    private String name;
    private String email;
    private List<Account> accounts = new ArrayList<>();

    public BankCustomerDocument() {
    }

    public BankCustomerDocument(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public void addAccount(Account account) {
        this.accounts.add(account);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

    public static class Account {

        private String accountNumber;
        private String type;

        @Field(targetType = FieldType.DECIMAL128)
        private BigDecimal balance;

        private List<Transaction> transactions = new ArrayList<>();

        public Account() {
        }

        public Account(String accountNumber, String type, BigDecimal balance) {
            this.accountNumber = accountNumber;
            this.type = type;
            this.balance = balance;
        }

        public void addTransaction(Transaction transaction) {
            this.transactions.add(transaction);
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

        public List<Transaction> getTransactions() {
            return transactions;
        }

        public void setTransactions(List<Transaction> transactions) {
            this.transactions = transactions;
        }
    }

    public static class Transaction {

        private String type;

        @Field(targetType = FieldType.DECIMAL128)
        private BigDecimal amount;

        private Instant timestamp;

        public Transaction() {
        }

        public Transaction(String type, BigDecimal amount, Instant timestamp) {
            this.type = type;
            this.amount = amount;
            this.timestamp = timestamp;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
        }
    }
}
