package com.mongodb.course.m19.bdd;

import com.mongodb.course.m19.account.service.AccountCommandService;
import com.mongodb.course.m19.projection.DashboardQueryService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountLifecycleSteps {

    @Autowired private AccountCommandService accountCommandService;
    @Autowired private DashboardQueryService dashboardQueryService;

    @When("開立帳戶 {string} 持有人 {string} 初始存款 {int} 元")
    public void openAccount(String accountId, String holder, int amount) {
        accountCommandService.openAccount(accountId, holder, BigDecimal.valueOf(amount), "TWD");
    }

    @Given("已開立帳戶 {string} 初始存款 {int} 元")
    public void givenAccount(String accountId, int amount) {
        accountCommandService.openAccount(accountId, "持有人", BigDecimal.valueOf(amount), "TWD");
    }

    @When("存入 {int} 元到帳戶 {string} 備註 {string}")
    public void deposit(int amount, String accountId, String description) {
        accountCommandService.deposit(accountId, BigDecimal.valueOf(amount), description);
    }

    @When("從帳戶 {string} 提款 {int} 元")
    public void withdraw(String accountId, int amount) {
        accountCommandService.withdraw(accountId, BigDecimal.valueOf(amount), "提款");
    }

    @When("帳戶 {string} 加計利息 {int} 元")
    public void accrueInterest(String accountId, int amount) {
        accountCommandService.accrueInterest(accountId, BigDecimal.valueOf(amount));
    }

    @When("關閉帳戶 {string}")
    public void closeAccount(String accountId) {
        accountCommandService.closeAccount(accountId);
    }

    @Then("帳戶 {string} 餘額為 {int} 元")
    public void verifyBalance(String accountId, int expectedBalance) {
        var balance = dashboardQueryService.getAccountBalance(accountId);
        assertThat(balance).isEqualByComparingTo(BigDecimal.valueOf(expectedBalance));
    }

    @Then("帳戶摘要 {string} 交易次數為 {int}")
    public void verifyTransactionCount(String accountId, int expectedCount) {
        var summary = dashboardQueryService.getAccountSummary(accountId);
        assertThat(summary.totalTransactions()).isEqualTo(expectedCount);
    }

    @Then("帳戶摘要 {string} 累計利息為 {int} 元")
    public void verifyInterest(String accountId, int expectedInterest) {
        var summary = dashboardQueryService.getAccountSummary(accountId);
        assertThat(summary.totalInterestEarned()).isEqualByComparingTo(BigDecimal.valueOf(expectedInterest));
    }

    @Then("帳戶摘要 {string} 已標記關閉")
    public void verifyAccountClosed(String accountId) {
        var summary = dashboardQueryService.getAccountSummary(accountId);
        assertThat(summary.closed()).isTrue();
    }
}
