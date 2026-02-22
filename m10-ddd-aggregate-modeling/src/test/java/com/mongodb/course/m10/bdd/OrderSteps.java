package com.mongodb.course.m10.bdd;

import com.mongodb.course.m10.banking.domain.model.Money;
import com.mongodb.course.m10.ecommerce.application.OrderService;
import com.mongodb.course.m10.ecommerce.domain.model.Order;
import com.mongodb.course.m10.ecommerce.domain.model.OrderLine;
import com.mongodb.course.m10.ecommerce.domain.model.PaymentInfo;
import com.mongodb.course.m10.ecommerce.domain.model.ShippingAddress;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderSteps {

    @Autowired
    private OrderService orderService;

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final ShippingAddress DEFAULT_ADDRESS =
            new ShippingAddress("測試用戶", "中正路100號", "台北市", "100");

    private Order currentOrder;
    private Exception lastException;

    @Before
    public void setUpOrders() {
        mongoTemplate.dropCollection("m10_orders");
        currentOrder = null;
        lastException = null;
    }

    @When("建立訂單包含以下商品:")
    public void createOrderWithItems(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        List<OrderLine> lines = rows.stream()
                .map(row -> new OrderLine(
                        "P-" + row.get("商品名稱"),
                        row.get("商品名稱"),
                        Integer.parseInt(row.get("數量")),
                        Money.twd(Long.parseLong(row.get("單價")))))
                .toList();
        currentOrder = orderService.createOrder("ORD-001", "CUST-001", lines, DEFAULT_ADDRESS);
    }

    @Given("已建立包含商品總額 {int} 元的訂單")
    public void createOrderWithTotal(int totalAmount) {
        var lines = List.of(new OrderLine("P001", "商品", 1, Money.twd(totalAmount)));
        currentOrder = orderService.createOrder("ORD-001", "CUST-001", lines, DEFAULT_ADDRESS);
    }

    @When("執行付款交易編號 {string}")
    public void payOrder(String transactionId) {
        currentOrder = orderService.pay(currentOrder.getId(),
                new PaymentInfo("CREDIT_CARD", transactionId, Instant.now()));
    }

    @When("執行出貨物流編號 {string}")
    public void shipOrder(String trackingNumber) {
        currentOrder = orderService.ship(currentOrder.getId(), trackingNumber);
    }

    @When("確認送達")
    public void deliverOrder() {
        currentOrder = orderService.deliver(currentOrder.getId());
    }

    @When("完成訂單")
    public void completeOrder() {
        currentOrder = orderService.complete(currentOrder.getId());
    }

    @Given("訂單已付款")
    public void orderAlreadyPaid() {
        currentOrder = orderService.pay(currentOrder.getId(),
                new PaymentInfo("CREDIT_CARD", "TXN-AUTO", Instant.now()));
    }

    @When("嘗試新增商品 {string} 單價 {int} 元")
    public void tryAddLine(String productName, int unitPrice) {
        try {
            currentOrder = orderService.addLine(currentOrder.getId(),
                    new OrderLine("P-NEW", productName, 1, Money.twd(unitPrice)));
        } catch (Exception e) {
            lastException = e;
        }
    }

    @Given("訂單已付款已出貨已送達")
    public void orderPaidShippedDelivered() {
        currentOrder = orderService.pay(currentOrder.getId(),
                new PaymentInfo("CREDIT_CARD", "TXN-AUTO", Instant.now()));
        currentOrder = orderService.ship(currentOrder.getId(), "SHIP-AUTO");
        currentOrder = orderService.deliver(currentOrder.getId());
    }

    @When("申請退貨")
    public void requestReturn() {
        currentOrder = orderService.requestReturn(currentOrder.getId());
    }

    @Then("訂單狀態為 {string}")
    public void verifyOrderStatus(String status) {
        assertThat(currentOrder.getStatus().name()).isEqualTo(status);
    }

    @And("訂單總金額為 {int} 元")
    public void verifyOrderTotal(int total) {
        assertThat(currentOrder.getTotalAmount().amount())
                .isEqualByComparingTo(BigDecimal.valueOf(total));
    }

    @Then("操作失敗並回傳訂單不可修改錯誤")
    public void operationFailedOrderNotModifiable() {
        assertThat(lastException)
                .isInstanceOf(IllegalStateException.class);
    }
}
