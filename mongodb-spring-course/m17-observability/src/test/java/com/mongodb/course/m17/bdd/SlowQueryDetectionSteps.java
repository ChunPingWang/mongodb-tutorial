package com.mongodb.course.m17.bdd;

import com.mongodb.course.m17.banking.TransactionService;
import com.mongodb.course.m17.observability.SlowQueryDetector;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class SlowQueryDetectionSteps {

    @Autowired
    private SlowQueryDetector slowQueryDetector;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private MeterRegistry meterRegistry;

    @Given("慢查詢偵測器門檻值設定為 {int} 毫秒")
    public void setThreshold(int thresholdMs) {
        slowQueryDetector.setThresholdMs(thresholdMs);
    }

    @Given("慢查詢偵測器已清除歷史紀錄")
    public void clearHistory() {
        slowQueryDetector.clear();
    }

    @When("新增一筆帳戶 {string} 金額 {int} 元的交易")
    public void createTransaction(String accountId, int amount) {
        transactionService.create(accountId, amount, "DEPOSIT");
    }

    @When("查詢帳戶 {string} 的所有交易")
    public void queryByAccount(String accountId) {
        transactionService.findByAccountId(accountId);
    }

    @Then("慢查詢偵測器應捕獲至少 {int} 筆紀錄")
    public void verifyCapturedAtLeast(int minCount) {
        assertThat(slowQueryDetector.getCapturedCount()).isGreaterThanOrEqualTo(minCount);
    }

    @Then("慢查詢偵測器應捕獲 {int} 筆紀錄")
    public void verifyCapturedExactly(int count) {
        assertThat(slowQueryDetector.getCapturedCount()).isEqualTo(count);
    }

    @Then("捕獲的慢查詢應包含指令名稱 {string}")
    public void verifyCapturedCommandName(String commandName) {
        assertThat(slowQueryDetector.getCapturedQueries())
                .anyMatch(entry -> commandName.equals(entry.commandName()));
    }

    @Then("捕獲的慢查詢資料庫名稱不為空")
    public void verifyDatabaseNameNotEmpty() {
        assertThat(slowQueryDetector.getCapturedQueries())
                .allMatch(entry -> entry.databaseName() != null && !entry.databaseName().isEmpty());
    }

    @Then("MeterRegistry 應包含 {string} 計時器")
    public void verifyTimerExists(String metricName) {
        var timers = meterRegistry.find(metricName).timers();
        assertThat(timers).isNotEmpty();
    }
}
