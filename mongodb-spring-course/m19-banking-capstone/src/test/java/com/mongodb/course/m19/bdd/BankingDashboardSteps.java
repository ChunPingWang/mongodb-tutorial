package com.mongodb.course.m19.bdd;

import com.mongodb.course.m19.observability.SlowQueryDetector;
import com.mongodb.course.m19.projection.DashboardQueryService;
import com.mongodb.course.m19.projection.readmodel.AccountSummaryDocument;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BankingDashboardSteps {

    @Autowired private DashboardQueryService dashboardQueryService;
    @Autowired private SlowQueryDetector slowQueryDetector;

    private List<AccountSummaryDocument> rankingResult;
    private AccountSummaryDocument queriedSummary;

    @When("查詢餘額前 {int} 名帳戶")
    public void queryTopAccounts(int limit) {
        rankingResult = dashboardQueryService.topAccountsByBalance(limit);
    }

    @Then("排行結果依序為 {string} 和 {string}")
    public void verifyRanking(String first, String second) {
        assertThat(rankingResult).hasSizeGreaterThanOrEqualTo(2);
        assertThat(rankingResult.get(0).accountId()).isEqualTo(first);
        assertThat(rankingResult.get(1).accountId()).isEqualTo(second);
    }

    @When("查詢帳戶 {string} 各類型交易統計")
    public void queryAccountStats(String accountId) {
        queriedSummary = dashboardQueryService.getAccountSummary(accountId);
    }

    @Then("存款筆數為 {int}")
    public void verifyDepositCount(int expected) {
        assertThat(queriedSummary.depositCount()).isEqualTo(expected);
    }

    @Then("提款筆數為 {int}")
    public void verifyWithdrawalCount(int expected) {
        assertThat(queriedSummary.withdrawalCount()).isEqualTo(expected);
    }

    @Given("慢查詢偵測器門檻值設定為 {int} 毫秒")
    public void setSlowQueryThreshold(int thresholdMs) {
        slowQueryDetector.setThresholdMs(thresholdMs);
    }

    @Then("慢查詢偵測器應捕獲至少 {int} 筆紀錄")
    public void verifySlowQueryCount(int minCount) {
        assertThat(slowQueryDetector.getCapturedCount()).isGreaterThanOrEqualTo(minCount);
    }
}
