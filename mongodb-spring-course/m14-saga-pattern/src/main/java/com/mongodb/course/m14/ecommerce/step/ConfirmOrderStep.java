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

public final class ConfirmOrderStep implements SagaStep {

    private final MongoTemplate mongoTemplate;

    public ConfirmOrderStep(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public String name() {
        return "CONFIRM_ORDER";
    }

    @Override
    public void execute(SagaContext context) {
        String orderId = context.get("orderId", String.class);
        String productId = context.get("productId", String.class);
        int quantity = context.get("quantity", Integer.class);

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(orderId)),
                Update.update("status", OrderStatus.CONFIRMED),
                Order.class
        );

        // Items sold — release reservation
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(productId)),
                new Update().inc("reservedQuantity", -quantity),
                InventoryItem.class
        );
    }

    @Override
    public void compensate(SagaContext context) {
        String orderId = context.get("orderId", String.class);
        String productId = context.get("productId", String.class);
        int quantity = context.get("quantity", Integer.class);

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(orderId)),
                Update.update("status", OrderStatus.PAYMENT_PROCESSED),
                Order.class
        );

        // Re-reserve inventory
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(productId)),
                new Update().inc("reservedQuantity", quantity),
                InventoryItem.class
        );
    }
}
