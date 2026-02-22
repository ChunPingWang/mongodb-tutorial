package com.mongodb.course.m21.fulfillment.step;

import com.mongodb.course.m21.infrastructure.EventStore;
import com.mongodb.course.m21.infrastructure.saga.SagaContext;
import com.mongodb.course.m21.infrastructure.saga.SagaStep;
import com.mongodb.course.m21.order.event.OrderEvent;
import com.mongodb.course.m21.order.model.Order;
import com.mongodb.course.m21.projection.OrderDashboardProjector;
import com.mongodb.course.m21.projection.SalesStatisticsProjector;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConfirmOrderStep implements SagaStep {

    private static final String ORDER_EVENTS = "m21_order_events";

    private final EventStore eventStore;
    private final OrderDashboardProjector dashboardProjector;
    private final SalesStatisticsProjector statsProjector;

    public ConfirmOrderStep(EventStore eventStore,
                             OrderDashboardProjector dashboardProjector,
                             SalesStatisticsProjector statsProjector) {
        this.eventStore = eventStore;
        this.dashboardProjector = dashboardProjector;
        this.statsProjector = statsProjector;
    }

    @Override
    public String name() {
        return "CONFIRM_ORDER";
    }

    @Override
    public void execute(SagaContext context) {
        var order = (Order) context.get("order", Order.class);

        order.confirm();

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
        // Last step, no compensation needed
    }
}
