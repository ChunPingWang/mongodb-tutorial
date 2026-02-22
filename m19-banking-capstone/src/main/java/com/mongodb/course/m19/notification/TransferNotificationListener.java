package com.mongodb.course.m19.notification;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class TransferNotificationListener {

    private static final String EVENT_STORE = "m19_account_events";
    private static final String NOTIFICATIONS = "m19_transfer_notifications";

    private final MongoTemplate mongoTemplate;
    private final CopyOnWriteArrayList<TransferNotification> notifications = new CopyOnWriteArrayList<>();
    private DefaultMessageListenerContainer container;

    public TransferNotificationListener(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void start() {
        container = new DefaultMessageListenerContainer(mongoTemplate);
        container.start();

        MessageListener<ChangeStreamDocument<Document>, Document> listener = this::handleMessage;

        var csOptions = ChangeStreamOptions.builder().build();
        var requestOptions = new ChangeStreamRequestOptions(null, EVENT_STORE, csOptions);
        var request = new ChangeStreamRequest<>(listener, requestOptions);
        container.register(request, Document.class);
    }

    public void stop() {
        if (container != null && container.isRunning()) {
            container.stop();
        }
    }

    public List<TransferNotification> getNotifications() {
        return List.copyOf(notifications);
    }

    public void clear() {
        notifications.clear();
    }

    private void handleMessage(Message<ChangeStreamDocument<Document>, Document> message) {
        var body = message.getBody();
        if (body == null) return;

        String classType = body.getString("_class");
        if ("FundsTransferredOut".equals(classType) || "FundsTransferredIn".equals(classType)) {
            var notification = new TransferNotification(
                    body.getString("_id"),
                    body.getString("aggregateId"),
                    classType,
                    body.get("amount") != null
                            ? new BigDecimal(body.get("amount").toString())
                            : BigDecimal.ZERO,
                    Instant.now());
            notifications.add(notification);
            mongoTemplate.insert(notification, NOTIFICATIONS);
        }
    }
}
