package com.mongodb.course.m13.bdd;

import com.mongodb.course.m13.banking.command.BankAccountCommandService;
import com.mongodb.course.m13.banking.query.BankAccountQueryService;
import com.mongodb.course.m13.banking.readmodel.AccountSummaryDocument;
import com.mongodb.course.m13.banking.readmodel.TransactionHistoryDocument;
import com.mongodb.course.m13.projection.ProjectionRebuildService;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BankingCqrsSteps {

    @Autowired
    private BankAccountCommandService commandService;

    @Autowired
    private BankAccountQueryService queryService;

    @Autowired
    private ProjectionRebuildService rebuildService;

    @Autowired
    private MongoTemplate mongoTemplate;

    private List<TransactionHistoryDocument> transactionHistory;
    private List<AccountSummaryDocument> topAccounts;

    @Before
    public void cleanup() {
        mongoTemplate.remove(new Query(), "m13_account_events");
        mongoTemplate.remove(new Query(), "m13_account_summaries");
        mongoTemplate.remove(new Query(), "m13_transaction_history");
    }

    @When("開立帳戶 {string} 持有人 {string} 初始餘額 {int} 元")
    public void openAccount(String accountId, String holder, int balance) {
        commandService.openAccount(accountId, holder, new BigDecimal(balance), "TWD");
    }

    @Given("已開立帳戶 {string} 初始餘額 {int} 元")
    public void existingAccount(String accountId, int balance) {
        commandService.openAccount(accountId, "TestHolder", new BigDecimal(balance), "TWD");
    }

    @When("存入 {int} 元到帳戶 {string}")
    public void deposit(int amount, String accountId) {
        commandService.deposit(accountId, new BigDecimal(amount), "Deposit");
    }

    @When("提款 {int} 元從帳戶 {string}")
    public void withdraw(int amount, String accountId) {
        commandService.withdraw(accountId, new BigDecimal(amount), "Withdrawal");
    }

    @Then("帳戶摘要 {string} 餘額為 {int} 元")
    public void verifySummaryBalance(String accountId, int expectedBalance) {
        var summary = queryService.getAccountSummary(accountId).orElseThrow();
        assertThat(summary.currentBalance()).isEqualByComparingTo(new BigDecimal(expectedBalance));
    }

    @Then("帳戶摘要 {string} 交易次數為 {int}")
    public void verifySummaryTransactionCount(String accountId, int expectedCount) {
        var summary = queryService.getAccountSummary(accountId).orElseThrow();
        assertThat(summary.totalTransactionCount()).isEqualTo(expectedCount);
    }

    @Then("帳戶摘要 {string} 存款次數為 {int}")
    public void verifySummaryDepositCount(String accountId, int expectedCount) {
        var summary = queryService.getAccountSummary(accountId).orElseThrow();
        assertThat(summary.depositCount()).isEqualTo(expectedCount);
    }

    @Then("帳戶摘要 {string} 提款次數為 {int}")
    public void verifySummaryWithdrawalCount(String accountId, int expectedCount) {
        var summary = queryService.getAccountSummary(accountId).orElseThrow();
        assertThat(summary.withdrawalCount()).isEqualTo(expectedCount);
    }

    @When("查詢帳戶 {string} 交易歷史")
    public void queryTransactionHistory(String accountId) {
        transactionHistory = queryService.getTransactionHistory(accountId, 0, 100);
    }

    @Then("交易歷史包含 {int} 筆記錄")
    public void verifyTransactionHistoryCount(int expectedCount) {
        assertThat(transactionHistory).hasSize(expectedCount);
    }

    @Then("最後一筆交易餘額為 {int} 元")
    public void verifyLastTransactionBalance(int expectedBalance) {
        var last = transactionHistory.stream()
                .sorted((a, b) -> b.occurredAt().compareTo(a.occurredAt()))
                .findFirst().orElseThrow();
        assertThat(last.balanceAfter()).isEqualByComparingTo(new BigDecimal(expectedBalance));
    }

    @When("查詢餘額前 {int} 名帳戶")
    public void queryTopAccounts(int limit) {
        topAccounts = queryService.getTopAccountsByBalance(limit);
    }

    @Then("排行結果依序為 {string}")
    public void verifyTopAccountsOrder(String expectedOrder) {
        var expectedIds = expectedOrder.split(",");
        assertThat(topAccounts).hasSize(expectedIds.length);
        for (int i = 0; i < expectedIds.length; i++) {
            assertThat(topAccounts.get(i).accountId()).isEqualTo(expectedIds[i]);
        }
    }

    @When("清除銀行讀取模型")
    public void clearBankingReadModels() {
        rebuildService.clearBankingReadModels();
    }

    @Then("帳戶摘要 {string} 不存在")
    public void verifySummaryNotExists(String accountId) {
        var summary = queryService.getAccountSummary(accountId);
        assertThat(summary).isEmpty();
    }

    @When("重建銀行讀取模型")
    public void rebuildBankingProjections() {
        rebuildService.rebuildBankingProjections();
    }
}
