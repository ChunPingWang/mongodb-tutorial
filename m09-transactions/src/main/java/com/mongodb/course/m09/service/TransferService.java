package com.mongodb.course.m09.service;

import com.mongodb.course.m09.banking.AccountStatus;
import com.mongodb.course.m09.banking.BankAccount;
import com.mongodb.course.m09.banking.InsufficientBalanceException;
import com.mongodb.course.m09.banking.TransferRecord;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransferService {

    private final MongoTemplate mongoTemplate;

    public TransferService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Transactional
    public TransferRecord transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        BankAccount fromAccount = findAccountByNumber(fromAccountNumber);
        if (fromAccount == null) {
            throw new IllegalArgumentException("Source account not found: " + fromAccountNumber);
        }
        if (fromAccount.getStatus() == AccountStatus.FROZEN) {
            throw new IllegalStateException("Source account is frozen: " + fromAccountNumber);
        }
        if (fromAccount.getStatus() == AccountStatus.CLOSED) {
            throw new IllegalStateException("Source account is closed: " + fromAccountNumber);
        }

        BankAccount toAccount = findAccountByNumber(toAccountNumber);
        if (toAccount == null) {
            throw new IllegalArgumentException("Target account not found: " + toAccountNumber);
        }
        if (toAccount.getStatus() == AccountStatus.FROZEN) {
            throw new IllegalStateException("Target account is frozen: " + toAccountNumber);
        }
        if (toAccount.getStatus() == AccountStatus.CLOSED) {
            throw new IllegalStateException("Target account is closed: " + toAccountNumber);
        }

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance in account " + fromAccountNumber +
                            ": available=" + fromAccount.getBalance() + ", requested=" + amount);
        }

        // Debit source account
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("accountNumber").is(fromAccountNumber)),
                new Update().inc("balance", amount.negate()),
                BankAccount.class
        );

        // Credit target account
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("accountNumber").is(toAccountNumber)),
                new Update().inc("balance", amount),
                BankAccount.class
        );

        // Create transfer record
        TransferRecord record = new TransferRecord(
                fromAccountNumber, toAccountNumber, amount, TransferRecord.TransferStatus.SUCCESS
        );
        return mongoTemplate.insert(record);
    }

    public BankAccount findAccountByNumber(String accountNumber) {
        return mongoTemplate.findOne(
                Query.query(Criteria.where("accountNumber").is(accountNumber)),
                BankAccount.class
        );
    }

    public List<TransferRecord> getTransferHistory(String accountNumber) {
        Criteria criteria = new Criteria().orOperator(
                Criteria.where("fromAccountNumber").is(accountNumber),
                Criteria.where("toAccountNumber").is(accountNumber)
        );
        return mongoTemplate.find(Query.query(criteria), TransferRecord.class);
    }
}
