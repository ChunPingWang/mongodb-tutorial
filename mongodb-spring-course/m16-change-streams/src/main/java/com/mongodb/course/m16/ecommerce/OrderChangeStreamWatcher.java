package com.mongodb.course.m16.ecommerce;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.course.m16.infrastructure.ResumeTokenStore;
import org.bson.BsonDocument;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class OrderChangeStreamWatcher {

    private static final String COLLECTION = "m16_orders";
    private static final String LISTENER_NAME = "order-status-watcher";

    private final MongoClient mongoClient;
    private final MongoTemplate mongoTemplate;
    private final ResumeTokenStore resumeTokenStore;
    private final String databaseName;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final CountDownLatch readyLatch = new CountDownLatch(1);
    private Thread watcherThread;

    public OrderChangeStreamWatcher(MongoClient mongoClient,
                                    MongoTemplate mongoTemplate,
                                    ResumeTokenStore resumeTokenStore,
                                    @Value("${spring.data.mongodb.database:test}") String databaseName) {
        this.mongoClient = mongoClient;
        this.mongoTemplate = mongoTemplate;
        this.resumeTokenStore = resumeTokenStore;
        this.databaseName = databaseName;
    }

    public void startWatching() {
        startWatching(null);
    }

    public void startWatching(BsonDocument resumeToken) {
        if (running.getAndSet(true)) {
            return;
        }

        var latch = new CountDownLatch(1);

        watcherThread = new Thread(() -> {
            var pipeline = List.of(
                    Aggregates.match(Filters.and(
                            Filters.eq("operationType", "update"),
                            Filters.exists("updateDescription.updatedFields.status")
                    ))
            );

            var collection = mongoClient.getDatabase(databaseName).getCollection(COLLECTION);
            var watchBuilder = collection.watch(pipeline).fullDocument(FullDocument.UPDATE_LOOKUP);

            if (resumeToken != null) {
                watchBuilder = watchBuilder.resumeAfter(resumeToken);
            }

            try (var cursor = watchBuilder.cursor()) {
                latch.countDown();
                while (running.get()) {
                    var event = cursor.tryNext();
                    if (event != null) {
                        processEvent(event);
                    } else {
                        Thread.sleep(50);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                if (running.get()) {
                    throw new RuntimeException("Change stream watcher error", e);
                }
            }
        }, "order-change-stream-watcher");

        watcherThread.setDaemon(true);
        watcherThread.start();

        try {
            latch.await(5, TimeUnit.SECONDS);
            // Small delay to ensure cursor is fully ready
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void stopWatching() {
        running.set(false);
        if (watcherThread != null) {
            watcherThread.interrupt();
            try {
                watcherThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            watcherThread = null;
        }
    }

    private void processEvent(ChangeStreamDocument<Document> event) {
        var fullDocument = event.getFullDocument();
        if (fullDocument == null) return;

        var orderId = fullDocument.getObjectId("_id").toHexString();
        var statusStr = fullDocument.getString("status");
        var newStatus = OrderStatus.valueOf(statusStr);

        var auditEntry = OrderAuditEntry.of(orderId, newStatus);
        mongoTemplate.insert(auditEntry);

        var resumeToken = event.getResumeToken();
        if (resumeToken != null) {
            resumeTokenStore.saveToken(LISTENER_NAME, resumeToken);
        }
    }
}
