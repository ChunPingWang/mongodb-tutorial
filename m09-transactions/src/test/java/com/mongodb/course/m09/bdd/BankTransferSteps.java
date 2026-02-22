package com.mongodb.course.m09.bdd;

import com.mongodb.course.m09.banking.AccountType;
import com.mongodb.course.m09.banking.BankAccount;
import com.mongodb.course.m09.banking.InsufficientBalanceException;
import com.mongodb.course.m09.banking.TransferRecord;
import com.mongodb.course.m09.service.TransferService;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class BankTransferSteps {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private TransferService transferService;

    private Exception lastException;

    @Before
    public void setUp() {
        mongoTemplate.remove(new Query(), BankAccount.class);
        mongoTemplate.remove(new Query(), TransferRecord.class);
        lastException = null;
    }

    @Given("帳戶 {string} 餘額 {int} 元")
    public void accountWithBalance(String accountNumber, int balance) {
        mongoTemplate.insert(new BankAccount(accountNumber, "Holder-" + accountNumber,
                AccountType.SAVINGS, new BigDecimal(balance)));
    }

    @When("從 {string} 轉帳 {int} 元至 {string}")
    public void transfer(String from, int amount, String to) {
        try {
            transferService.transfer(from, to, new BigDecimal(amount));
        } catch (Exception e) {
            lastException = e;
        }
    }

    @Then("{string} 餘額為 {int} 元")
    public void verifyBalance(String accountNumber, int expected) {
        BankAccount account = transferService.findAccountByNumber(accountNumber);
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal(expected));
    }

    @Then("產生一筆轉帳記錄")
    public void transferRecordCreated() {
        long count = mongoTemplate.count(new Query(), TransferRecord.class);
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Then("轉帳失敗並回傳餘額不足錯誤")
    public void transferFailedInsufficientBalance() {
        assertThat(lastException).isInstanceOf(InsufficientBalanceException.class);
    }

    @Then("{string} 餘額維持 {int} 元")
    public void balanceUnchanged(String accountNumber, int expected) {
        BankAccount account = transferService.findAccountByNumber(accountNumber);
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal(expected));
    }
}
