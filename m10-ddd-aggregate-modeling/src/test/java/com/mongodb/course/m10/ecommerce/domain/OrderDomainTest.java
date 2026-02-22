package com.mongodb.course.m10.ecommerce.domain;

import com.mongodb.course.m10.banking.domain.model.Money;
import com.mongodb.course.m10.ecommerce.domain.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderDomainTest {

    private static final ShippingAddress ADDRESS =
            new ShippingAddress("Alice", "123 Main St", "Taipei", "100");

    @Test
    void create_computesTotalFromLines() {
        var lines = List.of(
                new OrderLine("P001", "手機", 1, Money.twd(30_000)),
                new OrderLine("P002", "保護殼", 2, Money.twd(500))
        );

        Order order = Order.create("ORD-001", "CUST-001", lines, ADDRESS);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getTotalAmount().amount())
                .isEqualByComparingTo(new BigDecimal("31000"));
        assertThat(order.getDomainEvents()).hasSize(1);
    }

    @Test
    void pay_fromCreated_transitionsToPaid() {
        Order order = createSampleOrder();

        order.pay(new PaymentInfo("CREDIT_CARD", "TXN-001", Instant.now()));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void ship_fromPaid_transitionsToShipped() {
        Order order = createSampleOrder();
        order.pay(new PaymentInfo("CREDIT_CARD", "TXN-001", Instant.now()));

        order.ship("SHIP-001");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.getTrackingNumber()).isEqualTo("SHIP-001");
    }

    @Test
    void fullLifecycle_created_to_completed() {
        Order order = createSampleOrder();

        order.pay(new PaymentInfo("CREDIT_CARD", "TXN-001", Instant.now()));
        order.ship("SHIP-001");
        order.deliver();
        order.complete();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void addLine_whenPaid_throwsException() {
        Order order = createSampleOrder();
        order.pay(new PaymentInfo("CREDIT_CARD", "TXN-001", Instant.now()));

        assertThatThrownBy(() ->
                order.addLine(new OrderLine("P003", "耳機", 1, Money.twd(3_000))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PAID");
    }

    @Test
    void requestReturn_fromDelivered_transitionsToReturned() {
        Order order = createSampleOrder();
        order.pay(new PaymentInfo("CREDIT_CARD", "TXN-001", Instant.now()));
        order.ship("SHIP-001");
        order.deliver();

        order.requestReturn();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURNED);
    }

    private Order createSampleOrder() {
        var lines = List.of(
                new OrderLine("P001", "商品A", 1, Money.twd(50_000))
        );
        return Order.create("ORD-001", "CUST-001", lines, ADDRESS);
    }
}
