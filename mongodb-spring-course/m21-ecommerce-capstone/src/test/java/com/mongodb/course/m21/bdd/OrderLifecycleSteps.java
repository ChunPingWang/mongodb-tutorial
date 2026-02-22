package com.mongodb.course.m21.bdd;

import com.mongodb.course.m21.infrastructure.EventStore;
import com.mongodb.course.m21.order.event.OrderEvent;
import com.mongodb.course.m21.order.model.*;
import com.mongodb.course.m21.order.service.OrderCommandService;
import com.mongodb.course.m21.product.ElectronicsProduct;
import com.mongodb.course.m21.product.ProductCatalogService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderLifecycleSteps {

    @Autowired private OrderCommandService orderCommandService;
    @Autowired private ProductCatalogService productCatalogService;
    @Autowired private EventStore eventStore;

    private Order currentOrder;
    private String currentOrderId;
    private final Map<String, String> skuToProductId = new HashMap<>();

    @Given("商品 {string} 名稱 {string} 類別 {string} 價格 {int} 元庫存 {int} 件")
    public void createProduct(String sku, String name, String category, int price, int stock) {
        var product = new ElectronicsProduct(sku, sku, name, category,
                new BigDecimal(price), stock, "Generic", 12);
        productCatalogService.save(product);
        skuToProductId.put(sku, sku);
    }

    @When("客戶 {string} 下單購買 {int} 件 {string} 和 {int} 件 {string} 寄送至 {string}")
    public void placeOrderTwoProducts(String customerId, int qty1, String sku1,
                                       int qty2, String sku2, String city) {
        var p1 = productCatalogService.findBySku(sku1).orElseThrow();
        var p2 = productCatalogService.findBySku(sku2).orElseThrow();
        var lines = List.of(
                new OrderLine(p1.getId(), p1.getName(), p1.getCategory(), qty1, p1.getPrice()),
                new OrderLine(p2.getId(), p2.getName(), p2.getCategory(), qty2, p2.getPrice())
        );
        var address = new ShippingAddress("Customer", "Main St", city, "100");
        currentOrderId = "ORD-BDD-" + System.nanoTime();
        currentOrder = orderCommandService.placeOrder(currentOrderId, customerId, lines, address);
    }

    @Then("訂單狀態為 {string}")
    public void verifyOrderStatus(String status) {
        assertThat(currentOrder.getStatus().name()).isEqualTo(status);
    }

    @Then("訂單總金額為 {int} 元")
    public void verifyTotalAmount(int amount) {
        assertThat(currentOrder.getTotalAmount()).isEqualByComparingTo(new BigDecimal(amount));
    }

    @Then("事件數量為 {int}")
    public void verifyEventCount(int count) {
        assertThat(eventStore.countEvents(currentOrderId, "m21_order_events")).isEqualTo(count);
    }

    @Given("已建立並確認訂單 {string}")
    public void createAndConfirmOrder(String orderId) {
        var product = new ElectronicsProduct("ITEM-C", "ITEM-C", "Item", "Electronics",
                new BigDecimal("10000"), 100, "Brand", 12);
        productCatalogService.save(product);

        var lines = List.of(new OrderLine("ITEM-C", "Item", "Electronics", 1, new BigDecimal("10000")));
        var address = new ShippingAddress("User", "St", "Taipei", "100");
        orderCommandService.placeOrder(orderId, "CUST-C", lines, address);
        orderCommandService.reserveInventory(orderId, List.of("ITEM-C"));
        orderCommandService.processPayment(orderId,
                new PaymentInfo("TXN-C", "CREDIT_CARD", "0000"), new BigDecimal("10000"));
        orderCommandService.confirm(orderId);
        currentOrderId = orderId;
    }

    @When("訂單 {string} 以追蹤號碼 {string} 出貨")
    public void shipOrder(String orderId, String trackingNumber) {
        currentOrder = orderCommandService.ship(orderId, trackingNumber);
        currentOrderId = orderId;
    }

    @Then("訂單 {string} 狀態為 {string}")
    public void verifyNamedOrderStatus(String orderId, String status) {
        var order = orderCommandService.loadOrder(orderId);
        assertThat(order.getStatus().name()).isEqualTo(status);
    }

    @Given("已下單訂單 {string} 狀態為 {string}")
    public void createPlacedOrder(String orderId, String status) {
        var product = new ElectronicsProduct("ITEM-P", "ITEM-P", "Item", "Electronics",
                new BigDecimal("5000"), 100, "Brand", 12);
        productCatalogService.save(product);

        var lines = List.of(new OrderLine("ITEM-P", "Item", "Electronics", 1, new BigDecimal("5000")));
        var address = new ShippingAddress("User", "St", "Taipei", "100");
        currentOrder = orderCommandService.placeOrder(orderId, "CUST-P", lines, address);
        currentOrderId = orderId;
    }

    @When("取消訂單 {string} 原因 {string}")
    public void cancelOrder(String orderId, String reason) {
        currentOrder = orderCommandService.cancel(orderId, reason);
        currentOrderId = orderId;
    }

    @Given("訂單 {string} 已經歷下單到確認的 {int} 個事件")
    public void createOrderWithEvents(String orderId, int eventCount) {
        var product = new ElectronicsProduct("ITEM-R", "ITEM-R", "Item", "Electronics",
                new BigDecimal("3000"), 100, "Brand", 12);
        productCatalogService.save(product);

        var lines = List.of(new OrderLine("ITEM-R", "Item", "Electronics", 1, new BigDecimal("3000")));
        var address = new ShippingAddress("User", "St", "Taipei", "100");
        orderCommandService.placeOrder(orderId, "CUST-R", lines, address);
        orderCommandService.reserveInventory(orderId, List.of("ITEM-R"));
        orderCommandService.processPayment(orderId,
                new PaymentInfo("TXN-R", "CREDIT_CARD", "0000"), new BigDecimal("3000"));
        orderCommandService.confirm(orderId);
        currentOrderId = orderId;
    }

    @When("從事件重播訂單 {string}")
    public void replayOrder(String orderId) {
        var events = eventStore.loadEvents(orderId, OrderEvent.class, "m21_order_events");
        currentOrder = Order.replayFrom(events);
        currentOrderId = orderId;
    }

    @Then("重建訂單狀態為 {string}")
    public void verifyReplayedStatus(String status) {
        assertThat(currentOrder.getStatus().name()).isEqualTo(status);
    }
}
