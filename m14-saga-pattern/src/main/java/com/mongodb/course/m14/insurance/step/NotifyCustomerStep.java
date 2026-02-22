package com.mongodb.course.m14.insurance.step;

import com.mongodb.course.m14.insurance.model.Claim;
import com.mongodb.course.m14.insurance.model.ClaimSettlementStatus;
import com.mongodb.course.m14.insurance.model.Notification;
import com.mongodb.course.m14.insurance.model.NotificationStatus;
import com.mongodb.course.m14.saga.SagaContext;
import com.mongodb.course.m14.saga.SagaStep;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.UUID;

public final class NotifyCustomerStep implements SagaStep {

    private static final String FAIL_PREFIX = "FAIL_";

    private final MongoTemplate mongoTemplate;

    public NotifyCustomerStep(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public String name() {
        return "NOTIFY_CUSTOMER";
    }

    @Override
    public void execute(SagaContext context) {
        String claimId = context.get("claimId", String.class);
        String claimantName = context.get("claimantName", String.class);

        if (claimantName.startsWith(FAIL_PREFIX)) {
            throw new IllegalStateException("Notification failed for recipient: " + claimantName);
        }

        String notificationId = UUID.randomUUID().toString();
        var notification = new Notification(
                notificationId, claimantName, claimId,
                "Your claim " + claimId + " has been settled.",
                NotificationStatus.SENT
        );
        mongoTemplate.save(notification);

        context.put("notificationId", notificationId);

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(claimId)),
                Update.update("settlementStatus", ClaimSettlementStatus.NOTIFIED),
                Claim.class
        );
    }

    @Override
    public void compensate(SagaContext context) {
        String notificationId = context.get("notificationId", String.class);
        if (notificationId != null) {
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(notificationId)),
                    Update.update("status", NotificationStatus.FAILED),
                    Notification.class
            );
        }
    }
}
