package com.mongodb.course.m05.bdd;

import com.mongodb.course.m05.banking.AccountStatus;
import com.mongodb.course.m05.banking.AccountType;
import com.mongodb.course.m05.banking.BankAccount;
import com.mongodb.course.m05.banking.BankAccountRepository;
import com.mongodb.course.m05.banking.BankAccountService;
import com.mongodb.course.m05.ecommerce.Product;
import com.mongodb.course.m05.ecommerce.ProductRepository;
import com.mongodb.course.m05.ecommerce.ProductService;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MongoTemplateOperationsSteps {

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    private BankAccount currentAccount;
    private Product currentProduct;

    // --- Scenario 1: Deposit with $inc ---

    @Given("a bank account {string} for {string} with initial balance {double}")
    public void aBankAccountWithInitialBalance(String accountNumber, String holder, double balance) {
        currentAccount = new BankAccount(accountNumber, holder, AccountType.SAVINGS,
                BigDecimal.valueOf(balance));
        currentAccount = bankAccountRepository.save(currentAccount);
    }

    @When("I deposit {double} via MongoTemplate")
    public void iDepositViaMongoTemplate(double amount) {
        bankAccountService.deposit(currentAccount.getId(), BigDecimal.valueOf(amount));
    }

    @Then("the account balance becomes {double}")
    public void theAccountBalanceBecomes(double expectedBalance) {
        BankAccount found = bankAccountRepository.findById(currentAccount.getId()).orElseThrow();
        assertThat(found.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(expectedBalance));
    }

    @And("the account status remains {string}")
    public void theAccountStatusRemains(String expectedStatus) {
        BankAccount found = bankAccountRepository.findById(currentAccount.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(AccountStatus.valueOf(expectedStatus));
    }

    // --- Scenario 2: Add tag with $push ---

    @Given("a product with SKU {string} named {string} with tags {string}")
    public void aProductWithTags(String sku, String name, String tags) {
        currentProduct = new Product(sku, name, "Electronics", new BigDecimal("999.99"), true);
        currentProduct.setTags(new java.util.ArrayList<>(List.of(tags.split(","))));
        currentProduct = productRepository.save(currentProduct);
    }

    @When("I add the tag {string} via MongoTemplate")
    public void iAddTheTagViaMongoTemplate(String tag) {
        productService.addTag(currentProduct.getId(), tag);
    }

    @Then("the product tags contain {string} and {string}")
    public void theProductTagsContain(String tag1, String tag2) {
        Product found = productRepository.findById(currentProduct.getId()).orElseThrow();
        assertThat(found.getTags()).contains(tag1, tag2);
    }

    // --- Scenario 3: Upsert product ---

    @Given("no product exists with SKU {string}")
    public void noProductExistsWithSku(String sku) {
        // Ensure clean state - already cleaned by @Before in RepositoryCrudSteps
        // but let's be explicit
        List<Product> existing = productRepository.findAll();
        existing.stream()
                .filter(p -> sku.equals(p.getSku()))
                .forEach(p -> productRepository.deleteById(p.getId()));
    }

    @When("I upsert a product with SKU {string} named {string} at price {double}")
    public void iUpsertAProduct(String sku, String name, double price) {
        productService.upsertProduct(sku, name, "Gadgets", BigDecimal.valueOf(price));
    }

    @Then("a product with SKU {string} exists in the database")
    public void aProductWithSkuExists(String sku) {
        List<Product> found = productRepository.findAll();
        assertThat(found).anyMatch(p -> sku.equals(p.getSku()));
    }

    @And("its name is {string}")
    public void itsNameIs(String expectedName) {
        List<Product> found = productRepository.findAll();
        assertThat(found).anyMatch(p -> expectedName.equals(p.getName()));
    }
}
