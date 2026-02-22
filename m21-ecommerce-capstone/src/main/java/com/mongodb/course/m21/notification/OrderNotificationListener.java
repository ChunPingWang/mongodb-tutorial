package com.mongodb.course.m21.notification;

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
public class OrderNotificationListener {

    private static final String DASHBOARD = "m21_order_dashboard";
    private static final String NOTIFICATIONS = "m21_order_notifications";

    private final MongoTemplate mongoTemplate;
    private final CopyOnWriteArrayList<OrderNotification> notifications = new CopyOnWriteArrayList<>();
    private DefaultMessageListenerContainer container;

    public OrderNotificationListener(MongoTemplate mongoTemplate) {
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

    public List<OrderNotification> getNotifications() {
        return List.copyOf(notifications);
    }

    public void clear() {
        notifications.clear();
    }

    private void handleMessage(Message<ChangeStreamDocument<Document>, Document> message) {
        var body = message.getBody();
        if (body == null) return;

        String status = body.getString("status");
        if ("CONFIRMED".equals(status) || "SHIPPED".equals(status)) {
            String orderId = body.getString("_id");
            var notification = new OrderNotification(
                    UUID.randomUUID().toString(),
                    orderId,
                    status,
                    "Order " + orderId + " status changed to " + status,
                    Instant.now());
            notifications.add(notification);
            mongoTemplate.insert(notification, NOTIFICATIONS);
        }
    }
}
