package com.mongodb.course.m21.bdd;

import com.mongodb.course.m21.fulfillment.OrderFulfillmentSagaService;
import com.mongodb.course.m21.infrastructure.saga.SagaLog;
import com.mongodb.course.m21.infrastructure.saga.SagaLogRepository;
import com.mongodb.course.m21.order.model.Order;
import com.mongodb.course.m21.order.model.OrderLine;
import com.mongodb.course.m21.order.model.ShippingAddress;
import com.mongodb.course.m21.order.service.OrderCommandService;
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

public class OrderFulfillmentSteps {

    @Autowired private OrderFulfillmentSagaService sagaService;
    @Autowired private OrderCommandService orderCommandService;
    @Autowired private ProductCatalogService productCatalogService;
    @Autowired private SagaLogRepository sagaLogRepository;

    private final Map<String, Order> orders = new HashMap<>();
    private String lastSagaId;

    @Given("已下單訂單 {string} 購買 {int} 件 {string}")
    public void createOrderWithProduct(String orderId, int qty, String sku) {
        var product = productCatalogService.findBySku(sku).orElseThrow();
        var lines = List.of(new OrderLine(product.getId(), product.getName(),
                product.getCategory(), qty, product.getPrice()));
        var address = new ShippingAddress("Customer", "St", "Taipei", "100");
        var order = orderCommandService.placeOrder(orderId, "CUST-SAGA", lines, address);
        orders.put(orderId, order);
    }

    @When("執行訂單履約 Saga {string}")
    public void executeSaga(String orderId) {
        var order = orders.get(orderId);
        lastSagaId = sagaService.executeFulfillment(order);
    }

    @Then("Saga 狀態為 {string}")
    public void verifySagaStatus(String status) {
        var sagaLog = sagaLogRepository.findById(lastSagaId).orElseThrow();
        assertThat(sagaLog.status().name()).isEqualTo(status);
    }

    @Then("商品 {string} 庫存為 {int} 件")
    public void verifyProductStock(String sku, int expectedStock) {
        var product = productCatalogService.findBySku(sku).orElseThrow();
        assertThat(product.getStockQuantity()).isEqualTo(expectedStock);
    }

    @Then("商品 {string} 庫存恢復為 {int} 件")
    public void verifyStockRestored(String sku, int expectedStock) {
        var product = productCatalogService.findBySku(sku).orElseThrow();
        assertThat(product.getStockQuantity()).isEqualTo(expectedStock);
    }

    @Then("Saga 日誌包含 {int} 個步驟")
    public void verifySagaStepCount(int stepCount) {
        var sagaLog = sagaLogRepository.findById(lastSagaId).orElseThrow();
        assertThat(sagaLog.steps()).hasSize(stepCount);
    }
}
