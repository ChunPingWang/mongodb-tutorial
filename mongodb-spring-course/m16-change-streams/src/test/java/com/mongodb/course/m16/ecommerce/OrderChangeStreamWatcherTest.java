package com.mongodb.course.m16.ecommerce;

import com.mongodb.course.m16.SharedContainersConfig;
import com.mongodb.course.m16.infrastructure.ResumeTokenStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(SharedContainersConfig.class)
class OrderChangeStreamWatcherTest {

    @Autowired
    private OrderChangeStreamWatcher watcher;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ResumeTokenStore resumeTokenStore;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection("m16_orders");
        mongoTemplate.dropCollection("m16_order_audit_log");
        mongoTemplate.dropCollection("m16_resume_tokens");
    }

    @AfterEach
    void tearDown() {
        watcher.stopWatching();
    }

    @Test
    void orderStatusChange_createsAuditEntry() {
        watcher.startWatching();

        var order = orderService.create("CUST-001", 10000);
        orderService.transitionStatus(order.id(), OrderStatus.CONFIRMED);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var entries = mongoTemplate.findAll(OrderAuditEntry.class);
            assertThat(entries).hasSize(1);
            assertThat(entries.getFirst().orderId()).isEqualTo(order.id());
            assertThat(entries.getFirst().newStatus()).isEqualTo(OrderStatus.CONFIRMED);
        });
    }

    @Test
    void multipleStatusTransitions_allTracked() {
        watcher.startWatching();

        var order = orderService.create("CUST-002", 20000);
        orderService.transitionStatus(order.id(), OrderStatus.CONFIRMED);
        orderService.transitionStatus(order.id(), OrderStatus.SHIPPED);
        orderService.transitionStatus(order.id(), OrderStatus.DELIVERED);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var entries = mongoTemplate.findAll(OrderAuditEntry.class);
            assertThat(entries).hasSize(3);
        });
    }

    @Test
    void resumeToken_savedAfterEvent() {
        watcher.startWatching();

        var order = orderService.create("CUST-003", 15000);
        orderService.transitionStatus(order.id(), OrderStatus.CONFIRMED);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var token = resumeTokenStore.loadToken("order-status-watcher");
            assertThat(token).isNotNull();
        });
    }

    @Test
    void resumeFromToken_catchesNewEvents() {
        watcher.startWatching();

        var order = orderService.create("CUST-004", 30000);
        orderService.transitionStatus(order.id(), OrderStatus.CONFIRMED);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var entries = mongoTemplate.findAll(OrderAuditEntry.class);
            assertThat(entries).hasSize(1);
        });

        watcher.stopWatching();

        // Event while watcher is down
        orderService.transitionStatus(order.id(), OrderStatus.SHIPPED);

        // Resume from saved token
        var savedToken = resumeTokenStore.loadToken("order-status-watcher");
        assertThat(savedToken).isNotNull();
        watcher.startWatching(savedToken);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var entries = mongoTemplate.findAll(OrderAuditEntry.class);
            assertThat(entries).hasSize(2);
        });
    }

    @Test
    void nonStatusUpdate_notTracked() {
        watcher.startWatching();

        var order = orderService.create("CUST-005", 25000);
        orderService.updateTotalAmount(order.id(), 30000);
        orderService.transitionStatus(order.id(), OrderStatus.CONFIRMED);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var entries = mongoTemplate.findAll(OrderAuditEntry.class);
            assertThat(entries).hasSize(1);
            assertThat(entries.getFirst().newStatus()).isEqualTo(OrderStatus.CONFIRMED);
        });
    }
}
