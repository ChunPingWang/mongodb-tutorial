package com.mongodb.course.m04;

import com.mongodb.course.m04.embedded.BankCustomerDocument;
import com.mongodb.course.m04.embedded.BankCustomerDocument.Account;
import com.mongodb.course.m04.embedded.BankCustomerDocument.Transaction;
import com.mongodb.course.m04.embedded.BankCustomerEmbeddedRepository;
import com.mongodb.course.m04.referenced.AccountRef;
import com.mongodb.course.m04.referenced.AccountRefRepository;
import com.mongodb.course.m04.referenced.CustomerRef;
import com.mongodb.course.m04.referenced.CustomerRefRepository;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M04-LAB-01: Compare embedding vs referencing patterns in MongoDB.
 */
@SpringBootTest
@Import(SharedContainersConfig.class)
class EmbeddingVsReferencingTest {

    @Autowired
    private BankCustomerEmbeddedRepository embeddedRepository;

    @Autowired
    private CustomerRefRepository customerRefRepository;

    @Autowired
    private AccountRefRepository accountRefRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        embeddedRepository.deleteAll();
        customerRefRepository.deleteAll();
        accountRefRepository.deleteAll();
    }

    @Test
    @DisplayName("Embedded: read customer with accounts in a single query")
    void embeddedSingleQueryRead() {
        // Given: customer with embedded accounts and transactions
        var customer = new BankCustomerDocument("Alice", "alice@bank.com");
        var savings = new Account("ACC-001", "savings", new BigDecimal("50000"));
        savings.addTransaction(new Transaction("deposit", new BigDecimal("50000"), Instant.now()));
        var checking = new Account("ACC-002", "checking", new BigDecimal("15000"));
        customer.addAccount(savings);
        customer.addAccount(checking);
        embeddedRepository.save(customer);

        // When: single query retrieves everything
        BankCustomerDocument found = embeddedRepository.findByName("Alice").getFirst();

        // Then: all data is in one document
        assertThat(found.getAccounts()).hasSize(2);
        assertThat(found.getAccounts().getFirst().getTransactions()).hasSize(1);
    }

    @Test
    @DisplayName("Referenced: requires multiple queries to assemble customer data")
    void referencedMultipleQueries() {
        // Given: customer and accounts stored in separate collections
        var customer = customerRefRepository.save(new CustomerRef("Bob", "bob@bank.com"));
        accountRefRepository.save(new AccountRef(customer.getId(), "ACC-101", "savings", new BigDecimal("30000")));
        accountRefRepository.save(new AccountRef(customer.getId(), "ACC-102", "checking", new BigDecimal("10000")));

        // When: need two separate queries
        CustomerRef foundCustomer = customerRefRepository.findById(customer.getId()).orElseThrow();
        List<AccountRef> accounts = accountRefRepository.findByCustomerId(foundCustomer.getId());

        // Then: data comes from two collections
        assertThat(foundCustomer.getName()).isEqualTo("Bob");
        assertThat(accounts).hasSize(2);
    }

    @Test
    @DisplayName("Embedded: update rewrites the entire document")
    void embeddedUpdateRewritesDocument() {
        // Given: customer with embedded account
        var customer = new BankCustomerDocument("Charlie", "charlie@bank.com");
        customer.addAccount(new Account("ACC-201", "savings", new BigDecimal("20000")));
        embeddedRepository.save(customer);

        // When: update account balance (must save entire customer document)
        BankCustomerDocument found = embeddedRepository.findByName("Charlie").getFirst();
        found.getAccounts().getFirst().setBalance(new BigDecimal("25000"));
        embeddedRepository.save(found);

        // Then: entire document is rewritten
        BankCustomerDocument updated = embeddedRepository.findByName("Charlie").getFirst();
        assertThat(updated.getAccounts().getFirst().getBalance())
                .isEqualByComparingTo(new BigDecimal("25000"));
    }

    @Test
    @DisplayName("Referenced: update touches only the account document")
    void referencedUpdateTouchesOnlyAccount() {
        // Given: customer and account in separate collections
        var customer = customerRefRepository.save(new CustomerRef("Diana", "diana@bank.com"));
        var account = accountRefRepository.save(
                new AccountRef(customer.getId(), "ACC-301", "savings", new BigDecimal("40000")));

        // When: update only the account document
        account.setBalance(new BigDecimal("45000"));
        accountRefRepository.save(account);

        // Then: customer document is untouched, only account was updated
        CustomerRef unchangedCustomer = customerRefRepository.findById(customer.getId()).orElseThrow();
        AccountRef updatedAccount = accountRefRepository.findById(account.getId()).orElseThrow();
        assertThat(unchangedCustomer.getName()).isEqualTo("Diana");
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("45000"));
    }

    @Test
    @DisplayName("Document growth: embedded documents increase parent size")
    void documentGrowthComparison() {
        // Given: customer with many transactions (embedded)
        var customer = new BankCustomerDocument("Eve", "eve@bank.com");
        var account = new Account("ACC-401", "savings", new BigDecimal("100000"));
        for (int i = 0; i < 100; i++) {
            account.addTransaction(new Transaction("deposit", new BigDecimal("1000"), Instant.now()));
        }
        customer.addAccount(account);
        embeddedRepository.save(customer);

        // Verify: the single document contains all 100 transactions
        Document rawDoc = mongoTemplate.getCollection("bank_customers_embedded")
                .find(new Document("name", "Eve")).first();
        assertThat(rawDoc).isNotNull();

        @SuppressWarnings("unchecked")
        List<Document> accounts = (List<Document>) rawDoc.get("accounts");
        @SuppressWarnings("unchecked")
        List<Document> transactions = (List<Document>) accounts.getFirst().get("transactions");
        assertThat(transactions).hasSize(100);

        // Referenced pattern: transactions would be in a separate collection,
        // so the customer document stays small regardless of transaction count
        var refCustomer = customerRefRepository.save(new CustomerRef("Eve-Ref", "eve-ref@bank.com"));
        Document refDoc = mongoTemplate.getCollection("customers_ref")
                .find(new Document("name", "Eve-Ref")).first();
        assertThat(refDoc).isNotNull();
        // Referenced customer document has no transactions array — stays compact
        assertThat(refDoc.containsKey("accounts")).isFalse();
    }
}
