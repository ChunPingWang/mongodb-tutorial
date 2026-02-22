package com.mongodb.course.m21.projection;

import com.mongodb.course.m21.order.event.*;
import com.mongodb.course.m21.projection.readmodel.OrderDashboardDocument;
import com.mongodb.course.m21.projection.readmodel.TimelineEntry;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderDashboardProjector {

    private static final String COLLECTION = "m21_order_dashboard";

    private final MongoTemplate mongoTemplate;

    public OrderDashboardProjector(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void project(OrderEvent event) {
        switch (event) {
            case OrderPlaced e -> {
                int itemCount = e.lines().stream().mapToInt(l -> l.quantity()).sum();
                List<String> categories = e.lines().stream()
                        .map(l -> l.category())
                        .distinct()
                        .toList();
                var timeline = new TimelineEntry("PLACED", e.occurredAt(),
                        "Order placed with " + e.lines().size() + " line items");
                var doc = new OrderDashboardDocument(
                        e.aggregateId(), e.customerId(), e.totalAmount(),
                        "PLACED", itemCount, categories,
                        e.shippingAddress().city(), null,
                        List.of(timeline), e.occurredAt(), e.version());
                mongoTemplate.insert(doc, COLLECTION);
            }
            case InventoryReserved e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var timeline = new TimelineEntry("INVENTORY_RESERVED", e.occurredAt(),
                        "Inventory reserved for " + e.reservedProductIds().size() + " products");
                var update = new Update()
                        .set("status", "INVENTORY_RESERVED")
                        .push("timeline", timeline)
                        .set("lastUpdatedAt", e.occurredAt())
                        .set("projectedVersion", e.version());
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
            case PaymentProcessed e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var timeline = new TimelineEntry("PAYMENT_PROCESSED", e.occurredAt(),
                        "Payment processed: " + e.amount());
                var update = new Update()
                        .set("status", "PAYMENT_PROCESSED")
                        .push("timeline", timeline)
                        .set("lastUpdatedAt", e.occurredAt())
                        .set("projectedVersion", e.version());
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
            case OrderConfirmed e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var timeline = new TimelineEntry("CONFIRMED", e.occurredAt(), "Order confirmed");
                var update = new Update()
                        .set("status", "CONFIRMED")
                        .push("timeline", timeline)
                        .set("lastUpdatedAt", e.occurredAt())
                        .set("projectedVersion", e.version());
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
            case OrderShipped e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var timeline = new TimelineEntry("SHIPPED", e.occurredAt(),
                        "Shipped with tracking: " + e.trackingNumber());
                var update = new Update()
                        .set("status", "SHIPPED")
                        .set("trackingNumber", e.trackingNumber())
                        .push("timeline", timeline)
                        .set("lastUpdatedAt", e.occurredAt())
                        .set("projectedVersion", e.version());
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
            case OrderCancelled e -> {
                var query = Query.query(Criteria.where("_id").is(e.aggregateId()));
                var timeline = new TimelineEntry("CANCELLED", e.occurredAt(),
                        "Cancelled: " + e.reason());
                var update = new Update()
                        .set("status", "CANCELLED")
                        .push("timeline", timeline)
                        .set("lastUpdatedAt", e.occurredAt())
                        .set("projectedVersion", e.version());
                mongoTemplate.updateFirst(query, update, COLLECTION);
            }
        }
    }
}
