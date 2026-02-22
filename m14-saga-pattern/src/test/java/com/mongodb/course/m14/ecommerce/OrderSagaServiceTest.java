package com.mongodb.course.m14.ecommerce;

import com.mongodb.course.m14.SharedContainersConfig;
import com.mongodb.course.m14.ecommerce.model.InventoryItem;
import com.mongodb.course.m14.ecommerce.model.Order;
import com.mongodb.course.m14.ecommerce.model.OrderStatus;
import com.mongodb.course.m14.ecommerce.model.Payment;
import com.mongodb.course.m14.ecommerce.service.OrderSagaService;
import com.mongodb.course.m14.saga.SagaLog;
import com.mongodb.course.m14.saga.SagaLogRepository;
import com.mongodb.course.m14.saga.SagaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class OrderSagaServiceTest {

    @Autowired
    private OrderSagaService orderSagaService;

    @Autowired
    private SagaLogRepository sagaLogRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanup() {
        mongoTemplate.remove(new Query(), "m14_saga_logs");
        mongoTemplate.remove(new Query(), "m14_orders");
        mongoTemplate.remove(new Query(), "m14_inventory");
        mongoTemplate.remove(new Query(), "m14_payments");
    }

    @Test
    void placeOrder_happyPath_orderConfirmed() {
        mongoTemplate.save(new InventoryItem("PROD-001", "Laptop", 10, 0));

        String sagaId = orderSagaService.placeOrder("CUST-001", "PROD-001", "Laptop", 2, 20000);

        var sagaLog = sagaLogRepository.findById(sagaId).orElseThrow();
        assertThat(sagaLog.status()).isEqualTo(SagaStatus.COMPLETED);

        var orders = mongoTemplate.findAll(Order.class);
        assertThat(orders).hasSize(1);
        assertThat(orders.getFirst().status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(orders.getFirst().totalAmount()).isEqualTo(40000);

        var inventory = mongoTemplate.findById("PROD-001", InventoryItem.class);
        assertThat(inventory.quantity()).isEqualTo(8);
        assertThat(inventory.reservedQuantity()).isEqualTo(0);
    }

    @Test
    void placeOrder_paymentFails_inventoryReleasedAndOrderCancelled() {
        mongoTemplate.save(new InventoryItem("PROD-002", "Phone", 5, 0));

        String sagaId = orderSagaService.placeOrder("CUST-002", "PROD-002", "Phone", 2, 60000);

        var sagaLog = sagaLogRepository.findById(sagaId).orElseThrow();
        assertThat(sagaLog.status()).isEqualTo(SagaStatus.COMPENSATED);

        var orders = mongoTemplate.findAll(Order.class);
        assertThat(orders).hasSize(1);
        assertThat(orders.getFirst().status()).isEqualTo(OrderStatus.CANCELLED);

        var inventory = mongoTemplate.findById("PROD-002", InventoryItem.class);
        assertThat(inventory.quantity()).isEqualTo(5);
        assertThat(inventory.reservedQuantity()).isEqualTo(0);
    }

    @Test
    void placeOrder_insufficientInventory_orderCancelled() {
        mongoTemplate.save(new InventoryItem("PROD-003", "Tablet", 1, 0));

        String sagaId = orderSagaService.placeOrder("CUST-003", "PROD-003", "Tablet", 5, 15000);

        var sagaLog = sagaLogRepository.findById(sagaId).orElseThrow();
        assertThat(sagaLog.status()).isEqualTo(SagaStatus.COMPENSATED);

        var orders = mongoTemplate.findAll(Order.class);
        assertThat(orders).hasSize(1);
        assertThat(orders.getFirst().status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void placeOrder_sagaLogRecordsAllSteps() {
        mongoTemplate.save(new InventoryItem("PROD-004", "Mouse", 100, 0));

        String sagaId = orderSagaService.placeOrder("CUST-004", "PROD-004", "Mouse", 1, 500);

        var sagaLog = sagaLogRepository.findById(sagaId).orElseThrow();
        assertThat(sagaLog.steps()).hasSize(4);
        assertThat(sagaLog.steps()).allMatch(s -> "SUCCEEDED".equals(s.status()));
        assertThat(sagaLog.steps().get(0).stepName()).isEqualTo("PLACE_ORDER");
        assertThat(sagaLog.steps().get(1).stepName()).isEqualTo("RESERVE_INVENTORY");
        assertThat(sagaLog.steps().get(2).stepName()).isEqualTo("PROCESS_PAYMENT");
        assertThat(sagaLog.steps().get(3).stepName()).isEqualTo("CONFIRM_ORDER");
    }
}
