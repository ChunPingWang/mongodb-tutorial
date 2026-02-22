package com.mongodb.course.m21.fulfillment.step;

import com.mongodb.course.m21.infrastructure.EventStore;
import com.mongodb.course.m21.infrastructure.saga.SagaContext;
import com.mongodb.course.m21.infrastructure.saga.SagaStep;
import com.mongodb.course.m21.order.event.OrderEvent;
import com.mongodb.course.m21.order.model.Order;
import com.mongodb.course.m21.order.model.OrderLine;
import com.mongodb.course.m21.projection.OrderDashboardProjector;
import com.mongodb.course.m21.projection.SalesStatisticsProjector;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReserveInventoryStep implements SagaStep {

    private static final String ORDER_EVENTS = "m21_order_events";
    private static final String PRODUCTS = "m21_products";

    private final EventStore eventStore;
    private final MongoTemplate mongoTemplate;
    private final OrderDashboardProjector dashboardProjector;
    private final SalesStatisticsProjector statsProjector;

    public ReserveInventoryStep(EventStore eventStore, MongoTemplate mongoTemplate,
                                 OrderDashboardProjector dashboardProjector,
                                 SalesStatisticsProjector statsProjector) {
        this.eventStore = eventStore;
        this.mongoTemplate = mongoTemplate;
        this.dashboardProjector = dashboardProjector;
        this.statsProjector = statsProjector;
    }

    @Override
    public String name() {
        return "RESERVE_INVENTORY";
    }

    @Override
    public void execute(SagaContext context) {
        var order = (Order) context.get("order", Order.class);

        for (OrderLine line : order.getLines()) {
            var query = Query.query(Criteria.where("_id").is(line.productId())
                    .and("stockQuantity").gte(line.quantity()));
            var update = new Update().inc("stockQuantity", -line.quantity());
            var result = mongoTemplate.updateFirst(query, update, PRODUCTS);
            if (result.getModifiedCount() == 0) {
                throw new IllegalStateException("Insufficient stock for product: " + line.productId());
            }
        }

        List<String> productIds = order.getLines().stream()
                .map(OrderLine::productId)
                .toList();
        order.reserveInventory(productIds);

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

        for (OrderLine line : replayedOrder.getLines()) {
            var query = Query.query(Criteria.where("_id").is(line.productId()));
            var update = new Update().inc("stockQuantity", line.quantity());
            mongoTemplate.updateFirst(query, update, PRODUCTS);
        }
    }
}
