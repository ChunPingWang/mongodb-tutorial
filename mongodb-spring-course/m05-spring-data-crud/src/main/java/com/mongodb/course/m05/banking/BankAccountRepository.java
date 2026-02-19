package com.mongodb.course.m05.banking;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface BankAccountRepository extends MongoRepository<BankAccount, String> {

    List<BankAccount> findByHolderName(String holderName);

    List<BankAccount> findByType(AccountType type);

    List<BankAccount> findByTypeAndStatus(AccountType type, AccountStatus status);

    List<BankAccount> findByBalanceGreaterThan(BigDecimal minBalance);

    List<BankAccount> findByBalanceBetween(BigDecimal min, BigDecimal max);

    long countByStatus(AccountStatus status);

    boolean existsByAccountNumber(String accountNumber);

    @Query("{ 'balance': { $gte: ?0 }, 'status': 'ACTIVE' }")
    List<BankAccount> findHighValueAccounts(BigDecimal minBalance);

    @Query(value = "{ 'holderName': ?0 }", fields = "{ 'accountNumber': 1, 'holderName': 1, 'balance': 1 }")
    List<BankAccount> findAccountSummaryByHolder(String holderName);
}
