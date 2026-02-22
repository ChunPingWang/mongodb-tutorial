package com.mongodb.course.m20.settlement.step;

import com.mongodb.course.m20.claim.service.ClaimCommandService;
import com.mongodb.course.m20.infrastructure.saga.SagaContext;
import com.mongodb.course.m20.infrastructure.saga.SagaStep;
import com.mongodb.course.m20.notification.ClaimStatusNotification;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class NotifySettlementStep implements SagaStep {

    private static final String NOTIFICATIONS = "m20_claim_notifications";

    private final ClaimCommandService claimCommandService;
    private final MongoTemplate mongoTemplate;

    public NotifySettlementStep(ClaimCommandService claimCommandService,
                                MongoTemplate mongoTemplate) {
        this.claimCommandService = claimCommandService;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public String name() {
        return "NOTIFY_SETTLEMENT";
    }

    @Override
    public void execute(SagaContext context) {
        String claimId = context.get("claimId", String.class);
        BigDecimal amount = new BigDecimal(context.get("approvedAmount", String.class));
        String sagaId = context.get("sagaId", String.class);

        // Pay the claim
        claimCommandService.pay(claimId, amount, "SAGA-" + sagaId);

        // Insert notification
        var notification = new ClaimStatusNotification(
                UUID.randomUUID().toString(),
                claimId,
                "PAID",
                "Claim settled: " + amount + " paid",
                Instant.now());
        mongoTemplate.insert(notification, NOTIFICATIONS);
    }

    @Override
    public void compensate(SagaContext context) {
        // Payment compensation is complex in real systems; no-op for this capstone
    }
}
