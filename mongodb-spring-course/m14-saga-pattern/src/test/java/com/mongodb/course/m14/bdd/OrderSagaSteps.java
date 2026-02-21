package com.mongodb.course.m14.bdd;

import com.mongodb.course.m14.ecommerce.model.InventoryItem;
import com.mongodb.course.m14.ecommerce.model.Order;
import com.mongodb.course.m14.ecommerce.model.OrderStatus;
import com.mongodb.course.m14.ecommerce.model.Payment;
import com.mongodb.course.m14.ecommerce.service.OrderSagaService;
import com.mongodb.course.m14.saga.SagaLog;
import com.mongodb.course.m14.saga.SagaLogRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderSagaSteps {

    @Autowired
    private OrderSagaService orderSagaService;

    @Autowired
    private SagaLogRepository sagaLogRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private String sagaId;

    @Before
    public void cleanup() {
        mongoTemplate.remove(new Query(), "m14_saga_logs");
        mongoTemplate.remove(new Query(), "m14_orders");
        mongoTemplate.remove(new Query(), "m14_inventory");
        mongoTemplate.remove(new Query(), "m14_payments");
        mongoTemplate.remove(new Query(), "m14_notifications");
        mongoTemplate.remove(new Query(), "m14_claims");
        mongoTemplate.remove(new Query(), "m14_policies");
    }

    @Given("商品 {string} 名稱 {string} 庫存為 {int}")
    public void createInventory(String productId, String productName, int quantity) {
        mongoTemplate.save(new InventoryItem(productId, productName, quantity, 0));
    }

    @When("客戶 {string} 下單購買 {string} 數量 {int} 單價 {int} 元")
    public void placeOrder(String customerId, String productId, int quantity, int unitPrice) {
        var inventory = mongoTemplate.findById(productId, InventoryItem.class);
        sagaId = orderSagaService.placeOrder(customerId, productId, inventory.productName(), quantity, unitPrice);
    }

    @Then("Saga 狀態為 {string}")
    public void verifySagaStatus(String expectedStatus) {
        var sagaLog = sagaLogRepository.findById(sagaId).orElseThrow();
        assertThat(sagaLog.status().name()).isEqualTo(expectedStatus);
    }

    @Then("訂單狀態為 {string}")
    public void verifyOrderStatus(String expectedStatus) {
        var orders = mongoTemplate.findAll(Order.class);
        assertThat(orders).hasSize(1);
        assertThat(orders.getFirst().status()).isEqualTo(OrderStatus.valueOf(expectedStatus));
    }

    @Then("商品 {string} 庫存為 {int}")
    public void verifyInventory(String productId, int expectedQuantity) {
        var inventory = mongoTemplate.findById(productId, InventoryItem.class);
        assertThat(inventory.quantity()).isEqualTo(expectedQuantity);
    }

    @Then("產生一筆金額 {int} 元的付款記錄")
    public void verifyPayment(int expectedAmount) {
        var payments = mongoTemplate.findAll(Payment.class);
        assertThat(payments).hasSize(1);
        assertThat(payments.getFirst().amount()).isEqualTo(expectedAmount);
    }

    @Then("Saga 日誌包含 {int} 個步驟")
    public void verifySagaLogStepCount(int expectedCount) {
        var sagaLog = sagaLogRepository.findById(sagaId).orElseThrow();
        assertThat(sagaLog.steps()).hasSize(expectedCount);
    }

    @Then("每個步驟狀態為 {string}")
    public void verifyAllStepsStatus(String expectedStatus) {
        var sagaLog = sagaLogRepository.findById(sagaId).orElseThrow();
        assertThat(sagaLog.steps()).allMatch(s -> expectedStatus.equals(s.status()));
    }

    @Then("Saga 日誌中 {string} 步驟狀態為 {string}")
    public void verifySagaLogStepStatus(String stepName, String expectedStatus) {
        var sagaLog = sagaLogRepository.findById(sagaId).orElseThrow();
        var step = sagaLog.steps().stream()
                .filter(s -> stepName.equals(s.stepName()))
                .findFirst()
                .orElseThrow();
        assertThat(step.status()).isEqualTo(expectedStatus);
    }
}
