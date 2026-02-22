package com.mongodb.course.m10.ecommerce.domain.model;

import com.mongodb.course.m10.banking.domain.model.Money;
import com.mongodb.course.m10.ecommerce.domain.event.OrderCompleted;
import com.mongodb.course.m10.ecommerce.domain.event.OrderCreated;
import com.mongodb.course.m10.ecommerce.domain.event.OrderPaid;
import com.mongodb.course.m10.ecommerce.domain.event.OrderShipped;
import com.mongodb.course.m10.ecommerce.domain.specification.OrderModificationAllowedSpec;
import com.mongodb.course.m10.shared.DomainEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root — Order with state machine.
 * Pure domain class with ZERO Spring/MongoDB dependencies.
 */
public class Order {

    private String id;
    private String orderNumber;
    private String customerId;
    private List<OrderLine> lines;
    private ShippingAddress shippingAddress;
    private PaymentInfo paymentInfo;
    private String trackingNumber;
    private OrderStatus status;
    private Money totalAmount;
    private Instant createdAt;
    private Instant updatedAt;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Order() {
    }

    // ── Business factory ────────────────────────────────────────────────
    public static Order create(String orderNumber, String customerId,
                               List<OrderLine> lines, ShippingAddress address) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one line");
        }
        var order = new Order();
        order.orderNumber = orderNumber;
        order.customerId = customerId;
        order.lines = new ArrayList<>(lines);
        order.shippingAddress = address;
        order.status = OrderStatus.CREATED;
        order.totalAmount = lines.stream()
                .map(OrderLine::lineTotal)
                .reduce(Money.twd(0), Money::add);
        order.createdAt = Instant.now();
        order.updatedAt = order.createdAt;
        order.registerEvent(new OrderCreated(
                null, orderNumber, order.totalAmount, Instant.now()));
        return order;
    }

    // ── Persistence reconstitution ──────────────────────────────────────
    public static Order reconstitute(String id, String orderNumber, String customerId,
                                     List<OrderLine> lines, ShippingAddress shippingAddress,
                                     PaymentInfo paymentInfo, String trackingNumber,
                                     OrderStatus status, Money totalAmount,
                                     Instant createdAt, Instant updatedAt) {
        var order = new Order();
        order.id = id;
        order.orderNumber = orderNumber;
        order.customerId = customerId;
        order.lines = new ArrayList<>(lines);
        order.shippingAddress = shippingAddress;
        order.paymentInfo = paymentInfo;
        order.trackingNumber = trackingNumber;
        order.status = status;
        order.totalAmount = totalAmount;
        order.createdAt = createdAt;
        order.updatedAt = updatedAt;
        return order;
    }

    // ── State transitions ───────────────────────────────────────────────
    public void pay(PaymentInfo paymentInfo) {
        transitionTo(OrderStatus.PAID);
        this.paymentInfo = paymentInfo;
        registerEvent(new OrderPaid(id, paymentInfo.transactionId(), Instant.now()));
    }

    public void ship(String trackingNumber) {
        transitionTo(OrderStatus.SHIPPED);
        this.trackingNumber = trackingNumber;
        registerEvent(new OrderShipped(id, trackingNumber, Instant.now()));
    }

    public void deliver() {
        transitionTo(OrderStatus.DELIVERED);
    }

    public void complete() {
        transitionTo(OrderStatus.COMPLETED);
        registerEvent(new OrderCompleted(id, Instant.now()));
    }

    public void requestReturn() {
        transitionTo(OrderStatus.RETURNED);
    }

    // ── Modification guards ─────────────────────────────────────────────
    public void addLine(OrderLine line) {
        var spec = new OrderModificationAllowedSpec();
        if (!spec.isSatisfiedBy(status)) {
            throw new IllegalStateException(
                    "Cannot add lines to order in status " + status);
        }
        lines.add(line);
        totalAmount = totalAmount.add(line.lineTotal());
        updatedAt = Instant.now();
    }

    public void updateShippingAddress(ShippingAddress address) {
        if (status != OrderStatus.CREATED && status != OrderStatus.PAID) {
            throw new IllegalStateException(
                    "Cannot update shipping address in status " + status);
        }
        this.shippingAddress = address;
        updatedAt = Instant.now();
    }

    // ── Private helpers ─────────────────────────────────────────────────
    private void transitionTo(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Cannot transition from " + status + " to " + target);
        }
        this.status = target;
        this.updatedAt = Instant.now();
    }

    private void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    // ── Domain events ───────────────────────────────────────────────────
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    // ── Accessors ───────────────────────────────────────────────────────
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderNumber() { return orderNumber; }
    public String getCustomerId() { return customerId; }
    public List<OrderLine> getLines() { return Collections.unmodifiableList(lines); }
    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public PaymentInfo getPaymentInfo() { return paymentInfo; }
    public String getTrackingNumber() { return trackingNumber; }
    public OrderStatus getStatus() { return status; }
    public Money getTotalAmount() { return totalAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
