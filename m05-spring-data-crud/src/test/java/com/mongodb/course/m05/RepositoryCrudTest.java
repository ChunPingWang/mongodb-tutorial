package com.mongodb.course.m05;

import com.mongodb.course.m05.banking.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class RepositoryCrudTest {

    @Autowired
    private BankAccountRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    private BankAccount createSavingsAccount(String accountNumber, String holder, BigDecimal balance) {
        return new BankAccount(accountNumber, holder, AccountType.SAVINGS, balance);
    }

    @Test
    @DisplayName("LAB-01-01: save and findById round-trip")
    void saveAndFindById() {
        BankAccount account = createSavingsAccount("ACC-001", "Alice", new BigDecimal("1000.00"));
        BankAccount saved = repository.save(account);

        Optional<BankAccount> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getHolderName()).isEqualTo("Alice");
        assertThat(found.get().getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("LAB-01-02: save generates an id automatically")
    void saveGeneratesId() {
        BankAccount account = createSavingsAccount("ACC-002", "Bob", new BigDecimal("500.00"));
        assertThat(account.getId()).isNull();

        BankAccount saved = repository.save(account);

        assertThat(saved.getId()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("LAB-01-03: saveAll and findAll batch operations")
    void saveAll_and_findAll() {
        List<BankAccount> accounts = List.of(
                createSavingsAccount("ACC-010", "Alice", new BigDecimal("1000.00")),
                createSavingsAccount("ACC-011", "Bob", new BigDecimal("2000.00")),
                createSavingsAccount("ACC-012", "Charlie", new BigDecimal("3000.00"))
        );

        repository.saveAll(accounts);

        List<BankAccount> all = repository.findAll();
        assertThat(all).hasSize(3);
    }

    @Test
    @DisplayName("LAB-01-04: count and existsById")
    void count_and_existsById() {
        BankAccount saved = repository.save(
                createSavingsAccount("ACC-020", "Alice", new BigDecimal("1000.00")));

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.existsById(saved.getId())).isTrue();
        assertThat(repository.existsById("non-existent-id")).isFalse();
    }

    @Test
    @DisplayName("LAB-01-05: deleteById removes the document")
    void deleteById() {
        BankAccount saved = repository.save(
                createSavingsAccount("ACC-030", "Alice", new BigDecimal("1000.00")));

        repository.deleteById(saved.getId());

        assertThat(repository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("LAB-01-06: deleteAll removes all documents")
    void deleteAll() {
        repository.saveAll(List.of(
                createSavingsAccount("ACC-040", "Alice", new BigDecimal("1000.00")),
                createSavingsAccount("ACC-041", "Bob", new BigDecimal("2000.00"))
        ));

        repository.deleteAll();

        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("LAB-01-07: save updates existing document (full replacement)")
    void save_updates_existing_document() {
        BankAccount saved = repository.save(
                createSavingsAccount("ACC-050", "Alice", new BigDecimal("1000.00")));

        saved.setBalance(new BigDecimal("1500.00"));
        saved.setHolderName("Alice Wang");
        repository.save(saved);

        BankAccount updated = repository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getBalance()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(updated.getHolderName()).isEqualTo("Alice Wang");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("LAB-01-08: existsByAccountNumber derived query")
    void existsByAccountNumber_derived() {
        repository.save(createSavingsAccount("ACC-060", "Alice", new BigDecimal("1000.00")));

        assertThat(repository.existsByAccountNumber("ACC-060")).isTrue();
        assertThat(repository.existsByAccountNumber("ACC-999")).isFalse();
    }

    @Test
    @DisplayName("LAB-01-09: findByHolderName derived query")
    void findByHolderName_derived() {
        repository.saveAll(List.of(
                createSavingsAccount("ACC-070", "Alice", new BigDecimal("1000.00")),
                createSavingsAccount("ACC-071", "Alice", new BigDecimal("2000.00")),
                createSavingsAccount("ACC-072", "Bob", new BigDecimal("3000.00"))
        ));

        List<BankAccount> aliceAccounts = repository.findByHolderName("Alice");

        assertThat(aliceAccounts).hasSize(2);
        assertThat(aliceAccounts).allMatch(a -> a.getHolderName().equals("Alice"));
    }

    @Test
    @DisplayName("LAB-01-10: countByStatus derived query")
    void countByStatus_derived() {
        BankAccount active = createSavingsAccount("ACC-080", "Alice", new BigDecimal("1000.00"));
        BankAccount closed = createSavingsAccount("ACC-081", "Bob", new BigDecimal("2000.00"));
        closed.setStatus(AccountStatus.CLOSED);

        repository.saveAll(List.of(active, closed));

        assertThat(repository.countByStatus(AccountStatus.ACTIVE)).isEqualTo(1);
        assertThat(repository.countByStatus(AccountStatus.CLOSED)).isEqualTo(1);
    }
}
