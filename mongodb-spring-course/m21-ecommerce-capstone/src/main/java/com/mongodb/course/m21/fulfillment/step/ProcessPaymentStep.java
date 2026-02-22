package com.mongodb.course.m21.fulfillment.step;

import com.mongodb.course.m21.infrastructure.EventStore;
import com.mongodb.course.m21.infrastructure.saga.SagaContext;
import com.mongodb.course.m21.infrastructure.saga.SagaStep;
import com.mongodb.course.m21.order.event.OrderEvent;
import com.mongodb.course.m21.order.model.Order;
import com.mongodb.course.m21.order.model.PaymentInfo;
import com.mongodb.course.m21.projection.OrderDashboardProjector;
import com.mongodb.course.m21.projection.SalesStatisticsProjector;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class ProcessPaymentStep implements SagaStep {

    private static final String ORDER_EVENTS = "m21_order_events";
    private static final BigDecimal PAYMENT_LIMIT = new BigDecimal("1000000");

    private final EventStore eventStore;
    private final OrderDashboardProjector dashboardProjector;
    private final SalesStatisticsProjector statsProjector;

    public ProcessPaymentStep(EventStore eventStore,
                               OrderDashboardProjector dashboardProjector,
                               SalesStatisticsProjector statsProjector) {
        this.eventStore = eventStore;
        this.dashboardProjector = dashboardProjector;
        this.statsProjector = statsProjector;
    }

    @Override
    public String name() {
        return "PROCESS_PAYMENT";
    }

    @Override
    public void execute(SagaContext context) {
        var order = (Order) context.get("order", Order.class);

        if (order.getTotalAmount().compareTo(PAYMENT_LIMIT) > 0) {
            throw new IllegalStateException("Payment amount exceeds limit: " + order.getTotalAmount());
        }

        var paymentInfo = new PaymentInfo(
                UUID.randomUUID().toString(), "CREDIT_CARD", "1234");
        order.processPayment(paymentInfo, order.getTotalAmount());

        var uncommitted = List.copyOf(order.getUncommittedEvents());
        order.clearUncommittedEvents();
        eventStore.appendAll(uncommitted, ORDER_EVENTS);

        for (var event : uncommitted) {
            dashboardProjector.project(event);
            statsProjector.project(event);
        }
    }

    @Override
    public void compensate(SagaContext context) {
        var order = (Order) context.get("order", Order.class);

        var events = eventStore.loadEvents(order.getOrderId(), OrderEvent.class, ORDER_EVENTS);
        var replayedOrder = Order.replayFrom(events);
        replayedOrder.cancel("Payment compensation: saga rollback");

        var uncommitted = List.copyOf(replayedOrder.getUncommittedEvents());
        replayedOrder.clearUncommittedEvents();
        eventStore.appendAll(uncommitted, ORDER_EVENTS);

        for (var event : uncommitted) {
            dashboardProjector.project(event);
            statsProjector.project(event);
        }
    }
}
