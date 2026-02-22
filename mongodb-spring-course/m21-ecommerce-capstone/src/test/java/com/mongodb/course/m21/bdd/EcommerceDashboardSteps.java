package com.mongodb.course.m21.bdd;

import com.mongodb.course.m21.observability.SlowQueryDetector;
import com.mongodb.course.m21.order.model.OrderLine;
import com.mongodb.course.m21.order.model.PaymentInfo;
import com.mongodb.course.m21.order.model.ShippingAddress;
import com.mongodb.course.m21.order.service.OrderCommandService;
import com.mongodb.course.m21.product.ProductCatalogService;
import com.mongodb.course.m21.projection.OrderQueryService;
import com.mongodb.course.m21.projection.readmodel.OrderDashboardDocument;
import com.mongodb.course.m21.projection.readmodel.SalesStatisticsDocument;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class EcommerceDashboardSteps {

    @Autowired private OrderCommandService orderCommandService;
    @Autowired private OrderQueryService orderQueryService;
    @Autowired private ProductCatalogService productCatalogService;
    @Autowired private SlowQueryDetector slowQueryDetector;

    private SalesStatisticsDocument currentStats;
    private OrderDashboardDocument currentDashboard;

    @When("查詢類別 {string} 的銷售統計")
    public void queryStatsByCategory(String category) {
        currentStats = orderQueryService.findStatisticsByCategory(category).orElse(null);
    }

    @Then("該類別總訂單數為 {int}")
    public void verifyTotalOrders(int expected) {
        assertThat(currentStats).isNotNull();
        assertThat(currentStats.totalOrders()).isEqualTo(expected);
    }

    @Then("該類別總營收為 {int} 元")
    public void verifyTotalRevenue(int expected) {
        assertThat(currentStats).isNotNull();
        assertThat(currentStats.totalRevenue()).isEqualByComparingTo(new BigDecimal(expected));
    }

    @Given("訂單 {string} 已保留庫存")
    public void reserveInventoryForOrder(String orderId) {
        var dashboard = orderQueryService.findDashboardByOrderId(orderId).orElseThrow();
        orderCommandService.reserveInventory(orderId,
                dashboard.categories().stream().map(c -> "product-" + c).toList());
    }

    @Given("訂單 {string} 已處理付款")
    public void processPaymentForOrder(String orderId) {
        orderCommandService.processPayment(orderId,
                new PaymentInfo("TXN-DASH", "CREDIT_CARD", "9999"),
                new BigDecimal("8000"));
    }

    @When("查詢訂單儀表板 {string}")
    public void queryDashboard(String orderId) {
        currentDashboard = orderQueryService.findDashboardByOrderId(orderId).orElse(null);
    }

    @Then("時間軸包含 {int} 筆紀錄")
    public void verifyTimelineCount(int expected) {
        assertThat(currentDashboard).isNotNull();
        assertThat(currentDashboard.timeline()).hasSize(expected);
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
