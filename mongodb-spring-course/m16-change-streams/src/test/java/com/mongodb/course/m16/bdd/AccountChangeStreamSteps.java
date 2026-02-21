package com.mongodb.course.m16.bdd;

import com.mongodb.course.m16.banking.AccountChangeStreamListener;
import com.mongodb.course.m16.banking.AccountNotification;
import com.mongodb.course.m16.banking.AccountService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Duration;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class AccountChangeStreamSteps {

    @Autowired
    private AccountChangeStreamListener listener;

    @Autowired
    private AccountService accountService;

    @Autowired
    private MongoTemplate mongoTemplate;

    private String lastAccountId;

    @Given("帳戶變更串流已啟動")
    public void startAccountChangeStream() {
        listener.startListening();
    }

    @Given("帳戶變更串流已停止")
    public void stopAccountChangeStream() {
        listener.stopListening();
    }

    @Given("帳戶變更串流已啟動僅監聽新增事件")
    public void startAccountChangeStreamInsertsOnly() {
        listener.startListeningForInsertsOnly();
    }

    @Given("已存在帳戶持有人 {string} 餘額 {long} 元")
    public void createExistingAccount(String holder, long balance) {
        var account = accountService.create(holder, balance);
        lastAccountId = account.id();
    }

    @When("新增帳戶持有人 {string} 餘額 {long} 元")
    public void insertAccount(String holder, long balance) {
        var account = accountService.create(holder, balance);
        lastAccountId = account.id();
    }

    @When("更新該帳戶餘額為 {long} 元")
    public void updateAccountBalance(long newBalance) {
        accountService.updateBalance(lastAccountId, newBalance);
    }

    @When("刪除該帳戶")
    public void deleteAccount() {
        accountService.delete(lastAccountId);
    }

    @When("等待 {long} 毫秒")
    public void waitMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Then("應收到 {int} 筆變更通知")
    public void shouldReceiveNotifications(int count) {
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var notifications = mongoTemplate.findAll(AccountNotification.class);
            assertThat(notifications).hasSize(count);
        });
    }

    @Then("最新通知操作類型為 {string}")
    public void latestNotificationOperationType(String expectedType) {
        var notifications = mongoTemplate.findAll(AccountNotification.class);
        var latest = notifications.stream()
                .max(Comparator.comparing(AccountNotification::capturedAt))
                .orElseThrow();
        assertThat(latest.operationType()).isEqualTo(expectedType);
    }

    @Then("最新通知帳戶持有人為 {string}")
    public void latestNotificationAccountHolder(String expectedHolder) {
        var notifications = mongoTemplate.findAll(AccountNotification.class);
        var latest = notifications.stream()
                .max(Comparator.comparing(AccountNotification::capturedAt))
                .orElseThrow();
        assertThat(latest.accountHolder()).isEqualTo(expectedHolder);
    }

    @Then("最新通知餘額為 {long} 元")
    public void latestNotificationBalance(long expectedBalance) {
        var notifications = mongoTemplate.findAll(AccountNotification.class);
        var latest = notifications.stream()
                .max(Comparator.comparing(AccountNotification::capturedAt))
                .orElseThrow();
        assertThat(latest.balance()).isEqualTo(expectedBalance);
    }

    @Then("仍然只有 {int} 筆變更通知")
    public void stillOnlyNotifications(int count) {
        var notifications = mongoTemplate.findAll(AccountNotification.class);
        assertThat(notifications).hasSize(count);
    }
}
