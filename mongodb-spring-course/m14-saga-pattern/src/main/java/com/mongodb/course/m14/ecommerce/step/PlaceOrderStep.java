package com.mongodb.course.m14.ecommerce.step;

import com.mongodb.course.m14.ecommerce.model.Order;
import com.mongodb.course.m14.ecommerce.model.OrderItem;
import com.mongodb.course.m14.ecommerce.model.OrderStatus;
import com.mongodb.course.m14.saga.SagaContext;
import com.mongodb.course.m14.saga.SagaStep;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;
import java.util.UUID;

public final class PlaceOrderStep implements SagaStep {

    private final MongoTemplate mongoTemplate;

    public PlaceOrderStep(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public String name() {
        return "PLACE_ORDER";
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(SagaContext context) {
        String customerId = context.get("customerId", String.class);
        String productId = context.get("productId", String.class);
        String productName = context.get("productName", String.class);
        int quantity = context.get("quantity", Integer.class);
        long unitPrice = context.get("unitPrice", Long.class);

        var item = new OrderItem(productId, productName, quantity, unitPrice);
        long totalAmount = item.subtotal();

        String orderId = UUID.randomUUID().toString();
        var order = new Order(orderId, customerId, List.of(item), totalAmount, OrderStatus.PENDING);
        mongoTemplate.save(order);

        context.put("orderId", orderId);
        context.put("totalAmount", totalAmount);
    }

    @Override
    public void compensate(SagaContext context) {
        String orderId = context.get("orderId", String.class);
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(orderId)),
                Update.update("status", OrderStatus.CANCELLED),
                Order.class
        );
    }
}
