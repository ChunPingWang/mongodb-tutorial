package com.mongodb.course.m21.projection;

import com.mongodb.course.m21.order.event.*;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.stream.Collectors;

@Service
public class SalesStatisticsProjector {

    private static final String COLLECTION = "m21_sales_statistics";

    private final MongoTemplate mongoTemplate;

    public SalesStatisticsProjector(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void project(OrderEvent event) {
        switch (event) {
            case OrderPlaced e -> {
                var categoryItems = e.lines().stream()
                        .collect(Collectors.groupingBy(
                                l -> l.category(),
                                Collectors.summingInt(l -> l.quantity())));
                for (var entry : categoryItems.entrySet()) {
                    var query = Query.query(Criteria.where("_id").is(entry.getKey()));
                    var update = new Update()
                            .inc("totalOrders", 1)
                            .inc("totalItemsSold", entry.getValue())
                            .inc("totalRevenue", new Decimal128(
                                    e.lines().stream()
                                            .filter(l -> l.category().equals(entry.getKey()))
                                            .map(l -> l.lineTotal())
                                            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)))
                            .set("lastUpdatedAt", Instant.now());
                    mongoTemplate.upsert(query, update, COLLECTION);
                }
            }
            case OrderConfirmed e -> {
                String category = getCategoryForOrder(e.aggregateId());
                if (category != null) {
                    var query = Query.query(Criteria.where("_id").is(category));
                    var update = new Update().inc("confirmedCount", 1);
                    mongoTemplate.upsert(query, update, COLLECTION);
                }
            }
            case OrderCancelled e -> {
                String category = getCategoryForOrder(e.aggregateId());
                if (category != null) {
                    var query = Query.query(Criteria.where("_id").is(category));
                    var update = new Update().inc("cancelledCount", 1);
                    mongoTemplate.upsert(query, update, COLLECTION);
                }
            }
            case InventoryReserved e -> {}
            case PaymentProcessed e -> {}
            case OrderShipped e -> {}
        }
    }

    private String getCategoryForOrder(String orderId) {
        var query = Query.query(Criteria.where("_id").is(orderId));
        var dashboard = mongoTemplate.findOne(query, org.bson.Document.class, "m21_order_dashboard");
        if (dashboard != null) {
            var categories = dashboard.getList("categories", String.class);
            if (categories != null && !categories.isEmpty()) {
                return categories.getFirst();
            }
        }
        return null;
    }
}
