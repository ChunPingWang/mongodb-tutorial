package com.mongodb.course.m21.projection;

import com.mongodb.course.m21.projection.readmodel.CategoryMetrics;
import com.mongodb.course.m21.projection.readmodel.OrderDashboardDocument;
import com.mongodb.course.m21.projection.readmodel.SalesStatisticsDocument;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class OrderQueryService {

    private static final String DASHBOARD = "m21_order_dashboard";
    private static final String STATISTICS = "m21_sales_statistics";

    private final MongoTemplate mongoTemplate;

    public OrderQueryService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Optional<OrderDashboardDocument> findDashboardByOrderId(String orderId) {
        return Optional.ofNullable(mongoTemplate.findById(orderId, OrderDashboardDocument.class, DASHBOARD));
    }

    public Optional<SalesStatisticsDocument> findStatisticsByCategory(String category) {
        return Optional.ofNullable(mongoTemplate.findById(category, SalesStatisticsDocument.class, STATISTICS));
    }

    public CategoryMetrics computeCategoryMetrics(String category) {
        var pipeline = List.of(
                new Document("$match", new Document("categories", category)),
                new Document("$group", new Document("_id", (Object) null)
                        .append("totalOrders", new Document("$sum", 1))
                        .append("cancelledOrders", new Document("$sum",
                                new Document("$cond", List.of(
                                        new Document("$eq", List.of("$status", "CANCELLED")), 1, 0))))
                        .append("avgOrderValue", new Document("$avg", "$totalAmount"))
                        .append("totalRevenue", new Document("$sum", "$totalAmount")))
        );

        var results = mongoTemplate.getCollection(DASHBOARD)
                .aggregate(pipeline)
                .into(new java.util.ArrayList<>());

        if (results.isEmpty()) {
            return new CategoryMetrics(category, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        var r = results.getFirst();
        return new CategoryMetrics(
                category,
                r.get("totalOrders", Number.class).intValue(),
                r.get("cancelledOrders", Number.class).intValue(),
                new BigDecimal(r.get("avgOrderValue").toString()),
                new BigDecimal(r.get("totalRevenue").toString())
        );
    }
}
