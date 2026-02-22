package com.mongodb.course.m21.order.model;

import com.mongodb.course.m21.order.event.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class Order {

    private String orderId;
    private String customerId;
    private List<OrderLine> lines;
    private ShippingAddress shippingAddress;
    private PaymentInfo paymentInfo;
    private String trackingNumber;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private long version;
    private final List<OrderEvent> uncommittedEvents = new ArrayList<>();

    private Order() {
    }

    public static Order place(String orderId, String customerId,
                               List<OrderLine> lines, ShippingAddress shippingAddress) {
        BigDecimal total = lines.stream()
                .map(OrderLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var order = new Order();
        var event = new OrderPlaced(
                UUID.randomUUID().toString(), orderId, 1, Instant.now(),
                customerId, lines, shippingAddress, total);
        order.apply(event);
        order.uncommittedEvents.add(event);
        return order;
    }

    public void reserveInventory(List<String> productIds) {
        if (status != OrderStatus.PLACED) {
            throw new IllegalStateException("Can only reserve inventory for PLACED orders, current: " + status);
        }
        var event = new InventoryReserved(
                UUID.randomUUID().toString(), orderId, version + 1, Instant.now(),
                productIds);
        apply(event);
        uncommittedEvents.add(event);
    }

    public void processPayment(PaymentInfo info, BigDecimal amount) {
        if (status != OrderStatus.INVENTORY_RESERVED) {
            throw new IllegalStateException("Can only process payment for INVENTORY_RESERVED orders, current: " + status);
        }
        var event = new PaymentProcessed(
                UUID.randomUUID().toString(), orderId, version + 1, Instant.now(),
                info, amount);
        apply(event);
        uncommittedEvents.add(event);
    }

    public void confirm() {
        if (status != OrderStatus.PAYMENT_PROCESSED) {
            throw new IllegalStateException("Can only confirm PAYMENT_PROCESSED orders, current: " + status);
        }
        var event = new OrderConfirmed(
                UUID.randomUUID().toString(), orderId, version + 1, Instant.now());
        apply(event);
        uncommittedEvents.add(event);
    }

    public void ship(String trackingNumber) {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Can only ship CONFIRMED orders, current: " + status);
        }
        var event = new OrderShipped(
                UUID.randomUUID().toString(), orderId, version + 1, Instant.now(),
                trackingNumber);
        apply(event);
        uncommittedEvents.add(event);
    }

    public void cancel(String reason) {
        if (status != OrderStatus.PLACED && status != OrderStatus.INVENTORY_RESERVED) {
            throw new IllegalStateException("Can only cancel PLACED or INVENTORY_RESERVED orders, current: " + status);
        }
        var event = new OrderCancelled(
                UUID.randomUUID().toString(), orderId, version + 1, Instant.now(),
                reason);
        apply(event);
        uncommittedEvents.add(event);
    }

    public static Order replayFrom(List<OrderEvent> events) {
        if (events.isEmpty()) {
            throw new IllegalArgumentException("Cannot replay from empty event list");
        }
        var order = new Order();
        events.forEach(order::apply);
        return order;
    }

    public void replayAfterSnapshot(List<OrderEvent> events) {
        events.forEach(this::apply);
    }

    public Map<String, Object> toSnapshot() {
        var state = new LinkedHashMap<String, Object>();
        state.put("orderId", orderId);
        state.put("customerId", customerId);
        state.put("totalAmount", totalAmount.toPlainString());
        state.put("status", status.name());
        state.put("version", version);
        if (trackingNumber != null) {
            state.put("trackingNumber", trackingNumber);
        }
        return state;
    }

    public static Order fromSnapshot(Map<String, Object> state) {
        var order = new Order();
        order.orderId = (String) state.get("orderId");
        order.customerId = (String) state.get("customerId");
        order.totalAmount = new BigDecimal((String) state.get("totalAmount"));
        order.status = OrderStatus.valueOf((String) state.get("status"));
        order.version = ((Number) state.get("version")).longValue();
        order.trackingNumber = (String) state.get("trackingNumber");
        return order;
    }

    private void apply(OrderEvent event) {
        switch (event) {
            case OrderPlaced e -> {
                this.orderId = e.aggregateId();
                this.customerId = e.customerId();
                this.lines = new ArrayList<>(e.lines());
                this.shippingAddress = e.shippingAddress();
                this.totalAmount = e.totalAmount();
                this.status = OrderStatus.PLACED;
            }
            case InventoryReserved e -> this.status = OrderStatus.INVENTORY_RESERVED;
            case PaymentProcessed e -> {
                this.paymentInfo = e.paymentInfo();
                this.status = OrderStatus.PAYMENT_PROCESSED;
            }
            case OrderConfirmed e -> this.status = OrderStatus.CONFIRMED;
            case OrderShipped e -> {
                this.trackingNumber = e.trackingNumber();
                this.status = OrderStatus.SHIPPED;
            }
            case OrderCancelled e -> this.status = OrderStatus.CANCELLED;
        }
        this.version = event.version();
    }

    public List<OrderEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    public void clearUncommittedEvents() {
        uncommittedEvents.clear();
    }

    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public List<OrderLine> getLines() { return lines; }
    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public PaymentInfo getPaymentInfo() { return paymentInfo; }
    public String getTrackingNumber() { return trackingNumber; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public long getVersion() { return version; }
}
