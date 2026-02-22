package com.mongodb.course.m20.notification;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import org.bson.Document;
import org.springframework.data.mongodb.core.ChangeStreamOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.messaging.ChangeStreamRequest;
import org.springframework.data.mongodb.core.messaging.ChangeStreamRequest.ChangeStreamRequestOptions;
import org.springframework.data.mongodb.core.messaging.DefaultMessageListenerContainer;
import org.springframework.data.mongodb.core.messaging.Message;
import org.springframework.data.mongodb.core.messaging.MessageListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ClaimStatusNotificationListener {

    private static final String DASHBOARD = "m20_claim_dashboard";
    private static final String NOTIFICATIONS = "m20_claim_notifications";

    private final MongoTemplate mongoTemplate;
    private final CopyOnWriteArrayList<ClaimStatusNotification> notifications = new CopyOnWriteArrayList<>();
    private DefaultMessageListenerContainer container;

    public ClaimStatusNotificationListener(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void start() {
        container = new DefaultMessageListenerContainer(mongoTemplate);
        container.start();

        MessageListener<ChangeStreamDocument<Document>, Document> listener = this::handleMessage;

        var csOptions = ChangeStreamOptions.builder().build();
        var requestOptions = new ChangeStreamRequestOptions(null, DASHBOARD, csOptions);
        var request = new ChangeStreamRequest<>(listener, requestOptions);
        container.register(request, Document.class);
    }

    public void stop() {
        if (container != null && container.isRunning()) {
            container.stop();
        }
    }

    public List<ClaimStatusNotification> getNotifications() {
        return List.copyOf(notifications);
    }

    public void clear() {
        notifications.clear();
    }

    private void handleMessage(Message<ChangeStreamDocument<Document>, Document> message) {
        var body = message.getBody();
        if (body == null) return;

        String status = body.getString("status");
        if ("APPROVED".equals(status) || "PAID".equals(status)) {
            String claimId = body.getString("_id");
            var notification = new ClaimStatusNotification(
                    UUID.randomUUID().toString(),
                    claimId,
                    status,
                    "Claim " + claimId + " status changed to " + status,
                    Instant.now());
            notifications.add(notification);
            mongoTemplate.insert(notification, NOTIFICATIONS);
        }
    }
}
