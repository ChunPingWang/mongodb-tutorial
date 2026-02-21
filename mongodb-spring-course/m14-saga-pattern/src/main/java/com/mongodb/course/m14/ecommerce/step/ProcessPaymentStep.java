package com.mongodb.course.m14.ecommerce.step;

import com.mongodb.course.m14.ecommerce.model.Order;
import com.mongodb.course.m14.ecommerce.model.OrderStatus;
import com.mongodb.course.m14.ecommerce.model.Payment;
import com.mongodb.course.m14.ecommerce.model.PaymentStatus;
import com.mongodb.course.m14.saga.SagaContext;
import com.mongodb.course.m14.saga.SagaStep;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.UUID;

public final class ProcessPaymentStep implements SagaStep {

    static final long PAYMENT_LIMIT = 100_000;

    private final MongoTemplate mongoTemplate;

    public ProcessPaymentStep(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public String name() {
        return "PROCESS_PAYMENT";
    }

    @Override
    public void execute(SagaContext context) {
        long totalAmount = context.get("totalAmount", Long.class);
        String orderId = context.get("orderId", String.class);
        String sagaId = context.get("sagaId", String.class);

        if (totalAmount >= PAYMENT_LIMIT) {
            throw new IllegalStateException("Payment amount " + totalAmount + " exceeds limit " + PAYMENT_LIMIT);
        }

        String paymentId = UUID.randomUUID().toString();
        var payment = new Payment(paymentId, sagaId, orderId, totalAmount, PaymentStatus.COMPLETED, "ORDER");
        mongoTemplate.save(payment);

        context.put("paymentId", paymentId);

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(orderId)),
                Update.update("status", OrderStatus.PAYMENT_PROCESSED),
                Order.class
        );
    }

    @Override
    public void compensate(SagaContext context) {
        String paymentId = context.get("paymentId", String.class);
        if (paymentId != null) {
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(paymentId)),
                    Update.update("status", PaymentStatus.REFUNDED),
                    Payment.class
            );
        }
    }
}
