package com.mongodb.course.m04.bdd;

import com.mongodb.course.m04.java23.*;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class Java23FeaturesSteps {

    @Autowired
    private FinancialProductService productService;

    @Autowired
    private MongoTemplate mongoTemplate;

    private SavingsAccount savedAccount;
    private List<SavingsAccount> retrievedAccounts;
    private String description;

    @Before
    public void cleanUp() {
        mongoTemplate.dropCollection("financial_products");
    }

    @Given("a savings account record with name {string} and rate {string}")
    public void aSavingsAccountRecordWithNameAndRate(String name, String rate) {
        var account = new SavingsAccount("sa-bdd-001", name, new BigDecimal("50000"), new BigDecimal(rate));
        savedAccount = productService.save(account);
    }

    @When("I retrieve the record from MongoDB")
    public void iRetrieveTheRecordFromMongoDB() {
        retrievedAccounts = productService.findAllSavings();
    }

    @Then("the record fields are correctly mapped")
    public void theRecordFieldsAreCorrectlyMapped() {
        assertThat(retrievedAccounts).hasSize(1);
        SavingsAccount found = retrievedAccounts.getFirst();
        assertThat(found.name()).isEqualTo(savedAccount.name());
        assertThat(found.interestRate()).isEqualByComparingTo(savedAccount.interestRate());
    }

    @Given("financial products of different types")
    public void financialProductsOfDifferentTypes() {
        productService.save(new SavingsAccount("sa-bdd-002", "Basic Savings", new BigDecimal("10000"), new BigDecimal("1.5")));
        productService.save(new FixedDeposit("fd-bdd-001", "1-Year FD", new BigDecimal("100000"), 12));
        productService.save(new InsurancePolicy("ip-bdd-001", "Life Plan", new BigDecimal("500000"), "term-life"));
    }

    @When("I describe each product using pattern matching")
    public void iDescribeEachProductUsingPatternMatching() {
        // Pattern matching is used internally by productService.describe()
    }

    @Then("the sealed interface switch expression handles all types")
    public void theSealedInterfaceSwitchHandlesAllTypes() {
        FinancialProduct savings = new SavingsAccount("test", "Test", new BigDecimal("1000"), new BigDecimal("2.0"));
        FinancialProduct fd = new FixedDeposit("test", "Test", new BigDecimal("1000"), 6);
        FinancialProduct ip = new InsurancePolicy("test", "Test", new BigDecimal("1000"), "health");

        assertThat(productService.describe(savings)).startsWith("Savings:");
        assertThat(productService.describe(fd)).startsWith("Fixed Deposit:");
        assertThat(productService.describe(ip)).startsWith("Insurance:");
    }
}
