package com.mongodb.course.m14.insurance.step;

import com.mongodb.course.m14.ecommerce.model.Payment;
import com.mongodb.course.m14.ecommerce.model.PaymentStatus;
import com.mongodb.course.m14.insurance.model.Claim;
import com.mongodb.course.m14.insurance.model.ClaimSettlementStatus;
import com.mongodb.course.m14.saga.SagaContext;
import com.mongodb.course.m14.saga.SagaStep;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.UUID;

public final class CreateSettlementPaymentStep implements SagaStep {

    private final MongoTemplate mongoTemplate;

    public CreateSettlementPaymentStep(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public String name() {
        return "CREATE_SETTLEMENT_PAYMENT";
    }

    @Override
    public void execute(SagaContext context) {
        String claimId = context.get("claimId", String.class);
        long amount = context.get("amount", Long.class);
        String sagaId = context.get("sagaId", String.class);

        String paymentId = UUID.randomUUID().toString();
        var payment = new Payment(paymentId, sagaId, claimId, amount, PaymentStatus.COMPLETED, "CLAIM_SETTLEMENT");
        mongoTemplate.save(payment);

        context.put("paymentId", paymentId);

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(claimId)),
                Update.update("settlementStatus", ClaimSettlementStatus.PAYMENT_CREATED),
                Claim.class
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
