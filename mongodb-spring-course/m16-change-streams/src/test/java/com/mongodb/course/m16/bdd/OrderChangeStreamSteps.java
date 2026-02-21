package com.mongodb.course.m16.bdd;

import com.mongodb.course.m16.ecommerce.OrderAuditEntry;
import com.mongodb.course.m16.ecommerce.OrderChangeStreamWatcher;
import com.mongodb.course.m16.ecommerce.OrderService;
import com.mongodb.course.m16.ecommerce.OrderStatus;
import com.mongodb.course.m16.infrastructure.ResumeTokenStore;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Duration;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class OrderChangeStreamSteps {

    @Autowired
    private OrderChangeStreamWatcher watcher;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ResumeTokenStore resumeTokenStore;

    @Autowired
    private MongoTemplate mongoTemplate;

    private String lastOrderId;

    @Given("訂單變更監視器已啟動")
    public void startOrderWatcher() {
        watcher.startWatching();
    }

    @Given("已建立客戶 {string} 金額 {long} 元的訂單")
    public void createOrder(String customerId, long amount) {
        var order = orderService.create(customerId, amount);
        lastOrderId = order.id();
    }

    @When("將訂單狀態轉換為 {string}")
    public void transitionOrderStatus(String statusStr) {
        var status = OrderStatus.valueOf(statusStr);
        orderService.transitionStatus(lastOrderId, status);
    }

    @When("更新訂單金額為 {long} 元")
    public void updateOrderAmount(long newAmount) {
        orderService.updateTotalAmount(lastOrderId, newAmount);
    }

    @When("訂單變更監視器已停止")
    public void stopOrderWatcher() {
        watcher.stopWatching();
    }

    @When("從恢復令牌重新啟動監視器")
    public void resumeFromToken() {
        var token = resumeTokenStore.loadToken("order-status-watcher");
        assertThat(token).isNotNull();
        watcher.startWatching(token);
    }

    @Then("稽核日誌應有 {int} 筆記錄")
    public void auditLogShouldHaveEntries(int count) {
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var entries = mongoTemplate.findAll(OrderAuditEntry.class);
            assertThat(entries).hasSize(count);
        });
    }

    @Then("最新稽核記錄狀態為 {string}")
    public void latestAuditEntryStatus(String expectedStatus) {
        var entries = mongoTemplate.findAll(OrderAuditEntry.class);
        var latest = entries.stream()
                .max(Comparator.comparing(OrderAuditEntry::changedAt))
                .orElseThrow();
        assertThat(latest.newStatus()).isEqualTo(OrderStatus.valueOf(expectedStatus));
    }

    @Then("恢復令牌應已儲存")
    public void resumeTokenShouldBeSaved() {
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var token = resumeTokenStore.loadToken("order-status-watcher");
            assertThat(token).isNotNull();
        });
    }
}
