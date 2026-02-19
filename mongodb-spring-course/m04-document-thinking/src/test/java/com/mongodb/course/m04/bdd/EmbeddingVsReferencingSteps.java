package com.mongodb.course.m04.bdd;

import com.mongodb.course.m04.embedded.BankCustomerDocument;
import com.mongodb.course.m04.embedded.BankCustomerDocument.Account;
import com.mongodb.course.m04.embedded.BankCustomerDocument.Transaction;
import com.mongodb.course.m04.embedded.BankCustomerEmbeddedRepository;
import com.mongodb.course.m04.referenced.AccountRef;
import com.mongodb.course.m04.referenced.AccountRefRepository;
import com.mongodb.course.m04.referenced.CustomerRef;
import com.mongodb.course.m04.referenced.CustomerRefRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class EmbeddingVsReferencingSteps {

    @Autowired
    private BankCustomerEmbeddedRepository embeddedRepository;

    @Autowired
    private CustomerRefRepository customerRefRepository;

    @Autowired
    private AccountRefRepository accountRefRepository;

    private BankCustomerDocument embeddedCustomer;
    private CustomerRef referencedCustomer;

    @Before
    public void cleanUp() {
        embeddedRepository.deleteAll();
        customerRefRepository.deleteAll();
        accountRefRepository.deleteAll();
    }

    @Given("a customer {string} with embedded accounts and transactions")
    public void aCustomerWithEmbeddedAccounts(String name) {
        var customer = new BankCustomerDocument(name, name.toLowerCase() + "@bank.com");
        var savings = new Account("ACC-001", "savings", new BigDecimal("50000"));
        savings.addTransaction(new Transaction("deposit", new BigDecimal("50000"), Instant.now()));
        customer.addAccount(savings);
        embeddedCustomer = embeddedRepository.save(customer);
    }

    @When("I query the embedded customer by name {string}")
    public void iQueryTheEmbeddedCustomerByName(String name) {
        embeddedCustomer = embeddedRepository.findByName(name).getFirst();
    }

    @Then("all accounts and transactions are returned in a single document")
    public void allAccountsAndTransactionsReturnedInSingleDocument() {
        assertThat(embeddedCustomer.getAccounts()).isNotEmpty();
        assertThat(embeddedCustomer.getAccounts().getFirst().getTransactions()).isNotEmpty();
    }

    @Given("a customer {string} with referenced accounts")
    public void aCustomerWithReferencedAccounts(String name) {
        referencedCustomer = customerRefRepository.save(
                new CustomerRef(name, name.toLowerCase() + "@bank.com"));
        accountRefRepository.save(
                new AccountRef(referencedCustomer.getId(), "ACC-101", "savings", new BigDecimal("30000")));
        accountRefRepository.save(
                new AccountRef(referencedCustomer.getId(), "ACC-102", "checking", new BigDecimal("10000")));
    }

    @When("I query the customer and then the accounts separately")
    public void iQueryTheCustomerAndThenTheAccountsSeparately() {
        referencedCustomer = customerRefRepository.findById(referencedCustomer.getId()).orElseThrow();
    }

    @Then("I need two separate queries to assemble the full data")
    public void iNeedTwoSeparateQueries() {
        assertThat(referencedCustomer).isNotNull();
        List<AccountRef> accounts = accountRefRepository.findByCustomerId(referencedCustomer.getId());
        assertThat(accounts).hasSize(2);
    }
}
