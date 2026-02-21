package com.mongodb.course.m14.ecommerce.step;

import com.mongodb.course.m14.ecommerce.model.InventoryItem;
import com.mongodb.course.m14.ecommerce.model.Order;
import com.mongodb.course.m14.ecommerce.model.OrderStatus;
import com.mongodb.course.m14.saga.SagaContext;
import com.mongodb.course.m14.saga.SagaStep;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public final class ReserveInventoryStep implements SagaStep {

    private final MongoTemplate mongoTemplate;

    public ReserveInventoryStep(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public String name() {
        return "RESERVE_INVENTORY";
    }

    @Override
    public void execute(SagaContext context) {
        String productId = context.get("productId", String.class);
        int quantity = context.get("quantity", Integer.class);
        String orderId = context.get("orderId", String.class);

        var inventory = mongoTemplate.findById(productId, InventoryItem.class);
        if (inventory == null || inventory.quantity() < quantity) {
            throw new IllegalStateException("Insufficient inventory for product: " + productId);
        }

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(productId)),
                new Update().inc("quantity", -quantity).inc("reservedQuantity", quantity),
                InventoryItem.class
        );

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(orderId)),
                Update.update("status", OrderStatus.INVENTORY_RESERVED),
                Order.class
        );
    }

    @Override
    public void compensate(SagaContext context) {
        String productId = context.get("productId", String.class);
        int quantity = context.get("quantity", Integer.class);

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(productId)),
                new Update().inc("quantity", quantity).inc("reservedQuantity", -quantity),
                InventoryItem.class
        );
    }
}
