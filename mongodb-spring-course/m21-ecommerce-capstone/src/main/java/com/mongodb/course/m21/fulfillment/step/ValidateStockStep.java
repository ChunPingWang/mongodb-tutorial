package com.mongodb.course.m21.fulfillment.step;

import com.mongodb.course.m21.infrastructure.saga.SagaContext;
import com.mongodb.course.m21.infrastructure.saga.SagaStep;
import com.mongodb.course.m21.order.model.Order;
import com.mongodb.course.m21.order.model.OrderLine;
import com.mongodb.course.m21.product.Product;
import com.mongodb.course.m21.product.ProductCatalogService;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class ValidateStockStep implements SagaStep {

    private final ProductCatalogService productCatalogService;
    private final MongoTemplate mongoTemplate;

    public ValidateStockStep(ProductCatalogService productCatalogService, MongoTemplate mongoTemplate) {
        this.productCatalogService = productCatalogService;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public String name() {
        return "VALIDATE_STOCK";
    }

    @Override
    public void execute(SagaContext context) {
        @SuppressWarnings("unchecked")
        var order = (Order) context.get("order", Order.class);

        for (OrderLine line : order.getLines()) {
            Product product = productCatalogService.findById(line.productId())
                    .orElseThrow(() -> new IllegalStateException("Product not found: " + line.productId()));
            if (product.getStockQuantity() < line.quantity()) {
                throw new IllegalStateException("Insufficient stock for product: " + line.productId()
                        + " (available: " + product.getStockQuantity() + ", requested: " + line.quantity() + ")");
            }
        }

        List<String> orderCategories = order.getLines().stream()
                .map(OrderLine::category)
                .distinct()
                .toList();

        var twentyFourHoursAgo = Date.from(Instant.now().minus(24, ChronoUnit.HOURS));
        var pipeline = List.of(
                new Document("$match", new Document("customerId", order.getCustomerId())
                        .append("categories", new Document("$in", orderCategories))
                        .append("lastUpdatedAt", new Document("$gte", twentyFourHoursAgo))),
                new Document("$count", "recentOrders")
        );

        var results = mongoTemplate.getCollection("m21_order_dashboard")
                .aggregate(pipeline)
                .into(new ArrayList<>());

        if (!results.isEmpty()) {
            int recentOrders = results.getFirst().getInteger("recentOrders");
            if (recentOrders > 3) {
                throw new IllegalStateException("Bulk purchase limit exceeded: " + recentOrders
                        + " orders in last 24 hours for same category");
            }
        }
    }

    @Override
    public void compensate(SagaContext context) {
        // Read-only step, no compensation needed
    }
}
