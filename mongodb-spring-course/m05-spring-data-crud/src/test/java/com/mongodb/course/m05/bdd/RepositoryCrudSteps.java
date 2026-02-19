package com.mongodb.course.m05.bdd;

import com.mongodb.course.m05.banking.AccountType;
import com.mongodb.course.m05.banking.BankAccount;
import com.mongodb.course.m05.banking.BankAccountRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryCrudSteps {

    @Autowired
    private BankAccountRepository repository;

    private BankAccount currentAccount;

    @Before
    public void cleanUp() {
        repository.deleteAll();
    }

    // --- Scenario 1: Create and retrieve ---

    @Given("a new bank account with number {string} for holder {string} with balance {double}")
    public void aNewBankAccount(String accountNumber, String holder, double balance) {
        currentAccount = new BankAccount(accountNumber, holder, AccountType.SAVINGS,
                BigDecimal.valueOf(balance));
    }

    @When("I save the account via repository")
    public void iSaveTheAccountViaRepository() {
        currentAccount = repository.save(currentAccount);
    }

    @Then("I can retrieve the account by its id")
    public void iCanRetrieveTheAccountByItsId() {
        assertThat(repository.findById(currentAccount.getId())).isPresent();
    }

    @And("the holder name is {string}")
    public void theHolderNameIs(String expectedName) {
        BankAccount found = repository.findById(currentAccount.getId()).orElseThrow();
        assertThat(found.getHolderName()).isEqualTo(expectedName);
    }

    @And("the balance is {double}")
    public void theBalanceIs(double expectedBalance) {
        BankAccount found = repository.findById(currentAccount.getId()).orElseThrow();
        assertThat(found.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(expectedBalance));
    }

    // --- Scenario 2: Update balance ---

    @Given("an existing bank account with number {string} for holder {string} with balance {double}")
    public void anExistingBankAccount(String accountNumber, String holder, double balance) {
        currentAccount = new BankAccount(accountNumber, holder, AccountType.SAVINGS,
                BigDecimal.valueOf(balance));
        currentAccount = repository.save(currentAccount);
    }

    @When("I update the balance to {double} and save via repository")
    public void iUpdateTheBalanceAndSave(double newBalance) {
        currentAccount.setBalance(BigDecimal.valueOf(newBalance));
        currentAccount = repository.save(currentAccount);
    }

    @Then("the persisted balance is {double}")
    public void thePersistedBalanceIs(double expectedBalance) {
        BankAccount found = repository.findById(currentAccount.getId()).orElseThrow();
        assertThat(found.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(expectedBalance));
    }

    @And("the holder name is still {string}")
    public void theHolderNameIsStill(String expectedName) {
        BankAccount found = repository.findById(currentAccount.getId()).orElseThrow();
        assertThat(found.getHolderName()).isEqualTo(expectedName);
    }

    // --- Scenario 3: Delete ---

    @When("I delete the account by its id")
    public void iDeleteTheAccountByItsId() {
        repository.deleteById(currentAccount.getId());
    }

    @Then("the account no longer exists in the database")
    public void theAccountNoLongerExists() {
        assertThat(repository.findById(currentAccount.getId())).isEmpty();
    }
}
