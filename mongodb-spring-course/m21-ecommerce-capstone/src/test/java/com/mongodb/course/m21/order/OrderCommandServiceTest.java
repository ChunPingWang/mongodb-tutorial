package com.mongodb.course.m21.order;

import com.mongodb.course.m21.SharedContainersConfig;
import com.mongodb.course.m21.infrastructure.EventStore;
import com.mongodb.course.m21.order.event.*;
import com.mongodb.course.m21.order.model.*;
import com.mongodb.course.m21.order.service.OrderCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class OrderCommandServiceTest {

    @Autowired private OrderCommandService orderCommandService;
    @Autowired private EventStore eventStore;
    @Autowired private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.remove(new Query(), "m21_order_events");
        mongoTemplate.remove(new Query(), "m21_snapshots");
        mongoTemplate.remove(new Query(), "m21_order_dashboard");
        mongoTemplate.remove(new Query(), "m21_sales_statistics");
    }

    @Test
    void placeOrder() {
        var lines = List.of(
                new OrderLine("P1", "Laptop", "Electronics", 1, new BigDecimal("35000")),
                new OrderLine("P2", "Mouse", "Electronics", 2, new BigDecimal("500")),
                new OrderLine("P3", "T-Shirt", "Clothing", 3, new BigDecimal("300"))
        );
        var address = new ShippingAddress("Alice", "Main St", "Taipei", "100");

        var order = orderCommandService.placeOrder("ORD-001", "CUST-001", lines, address);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("36900"));
        assertThat(eventStore.countEvents("ORD-001", "m21_order_events")).isEqualTo(1);
    }

    @Test
    void fullOrderLifecycle() {
        var lines = List.of(
                new OrderLine("P1", "Phone", "Electronics", 1, new BigDecimal("25000"))
        );
        var address = new ShippingAddress("Bob", "2nd Ave", "Taipei", "110");

        orderCommandService.placeOrder("ORD-LC", "CUST-002", lines, address);
        orderCommandService.reserveInventory("ORD-LC", List.of("P1"));
        orderCommandService.processPayment("ORD-LC",
                new PaymentInfo("TXN-001", "CREDIT_CARD", "5678"), new BigDecimal("25000"));
        orderCommandService.confirm("ORD-LC");
        var shipped = orderCommandService.ship("ORD-LC", "TW999888777");

        assertThat(shipped.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(shipped.getTrackingNumber()).isEqualTo("TW999888777");
        assertThat(eventStore.countEvents("ORD-LC", "m21_order_events")).isEqualTo(5);
    }

    @Test
    void replayFromEvents() {
        var lines = List.of(
                new OrderLine("P1", "Watch", "Electronics", 1, new BigDecimal("8000"))
        );
        var address = new ShippingAddress("Charlie", "3rd Rd", "Taipei", "105");

        orderCommandService.placeOrder("ORD-RP", "CUST-003", lines, address);
        orderCommandService.reserveInventory("ORD-RP", List.of("P1"));
        orderCommandService.processPayment("ORD-RP",
                new PaymentInfo("TXN-002", "DEBIT_CARD", "9012"), new BigDecimal("8000"));
        orderCommandService.confirm("ORD-RP");

        var events = eventStore.loadEvents("ORD-RP", OrderEvent.class, "m21_order_events");
        var replayed = Order.replayFrom(events);

        assertThat(replayed.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(replayed.getVersion()).isEqualTo(4);
        assertThat(replayed.getTotalAmount()).isEqualByComparingTo(new BigDecimal("8000"));
    }
}
