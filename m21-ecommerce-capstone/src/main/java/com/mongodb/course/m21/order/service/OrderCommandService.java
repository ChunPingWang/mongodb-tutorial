package com.mongodb.course.m21.order.service;

import com.mongodb.course.m21.infrastructure.EventStore;
import com.mongodb.course.m21.infrastructure.SnapshotDocument;
import com.mongodb.course.m21.order.event.OrderEvent;
import com.mongodb.course.m21.order.model.*;
import com.mongodb.course.m21.projection.OrderDashboardProjector;
import com.mongodb.course.m21.projection.SalesStatisticsProjector;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderCommandService {

    private static final String ORDER_EVENTS = "m21_order_events";
    private static final String AGGREGATE_TYPE = "Order";
    private static final int SNAPSHOT_INTERVAL = 10;

    private final EventStore eventStore;
    private final OrderDashboardProjector dashboardProjector;
    private final SalesStatisticsProjector statsProjector;

    public OrderCommandService(EventStore eventStore,
                               OrderDashboardProjector dashboardProjector,
                               SalesStatisticsProjector statsProjector) {
        this.eventStore = eventStore;
        this.dashboardProjector = dashboardProjector;
        this.statsProjector = statsProjector;
    }

    public Order placeOrder(String orderId, String customerId,
                            List<OrderLine> lines, ShippingAddress shippingAddress) {
        var order = Order.place(orderId, customerId, lines, shippingAddress);
        saveAndProject(order);
        return order;
    }

    public Order reserveInventory(String orderId, List<String> productIds) {
        var order = loadOrder(orderId);
        order.reserveInventory(productIds);
        saveAndProject(order);
        return order;
    }

    public Order processPayment(String orderId, PaymentInfo paymentInfo, BigDecimal amount) {
        var order = loadOrder(orderId);
        order.processPayment(paymentInfo, amount);
        saveAndProject(order);
        return order;
    }

    public Order confirm(String orderId) {
        var order = loadOrder(orderId);
        order.confirm();
        saveAndProject(order);
        return order;
    }

    public Order ship(String orderId, String trackingNumber) {
        var order = loadOrder(orderId);
        order.ship(trackingNumber);
        saveAndProject(order);
        return order;
    }

    public Order cancel(String orderId, String reason) {
        var order = loadOrder(orderId);
        order.cancel(reason);
        saveAndProject(order);
        return order;
    }

    public Order loadOrder(String orderId) {
        var snapshotOpt = eventStore.loadLatestSnapshot(orderId, AGGREGATE_TYPE);
        if (snapshotOpt.isPresent()) {
            var snapshot = snapshotOpt.get();
            var order = Order.fromSnapshot(snapshot.state());
            var newEvents = eventStore.loadEventsAfterVersion(
                    orderId, snapshot.version(), OrderEvent.class, ORDER_EVENTS);
            order.replayAfterSnapshot(newEvents);
            return order;
        }
        var events = eventStore.loadEvents(orderId, OrderEvent.class, ORDER_EVENTS);
        return Order.replayFrom(events);
    }

    private void saveAndProject(Order order) {
        var events = List.copyOf(order.getUncommittedEvents());
        order.clearUncommittedEvents();

        eventStore.appendAll(events, ORDER_EVENTS);

        for (var event : events) {
            dashboardProjector.project(event);
            statsProjector.project(event);
        }

        if (order.getVersion() % SNAPSHOT_INTERVAL == 0) {
            var snapshot = new SnapshotDocument(
                    UUID.randomUUID().toString(),
                    order.getOrderId(),
                    AGGREGATE_TYPE,
                    order.getVersion(),
                    Instant.now(),
                    order.toSnapshot());
            eventStore.saveSnapshot(snapshot);
        }
    }
}
