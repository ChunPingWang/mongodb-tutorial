package com.mongodb.course.m16.banking;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import org.bson.BsonDocument;
import org.bson.Document;
import org.springframework.data.mongodb.core.ChangeStreamOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.messaging.ChangeStreamRequest;
import org.springframework.data.mongodb.core.messaging.ChangeStreamRequest.ChangeStreamRequestOptions;
import org.springframework.data.mongodb.core.messaging.DefaultMessageListenerContainer;
import org.springframework.data.mongodb.core.messaging.Message;
import org.springframework.data.mongodb.core.messaging.MessageListener;
import org.springframework.data.mongodb.core.messaging.Subscription;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class AccountChangeStreamListener {

    private static final String COLLECTION = "m16_accounts";

    private final MongoTemplate mongoTemplate;
    private DefaultMessageListenerContainer container;
    private Subscription subscription;

    public AccountChangeStreamListener(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void startListening() {
        var options = ChangeStreamOptions.builder()
                .fullDocumentLookup(FullDocument.UPDATE_LOOKUP)
                .build();
        startListeningWithOptions(options);
    }

    public void startListeningForInsertsOnly() {
        var options = ChangeStreamOptions.builder()
                .fullDocumentLookup(FullDocument.UPDATE_LOOKUP)
                .filter(Aggregation.newAggregation(match(where("operationType").is("insert"))))
                .build();
        startListeningWithOptions(options);
    }

    public void stopListening() {
        if (container != null && container.isRunning()) {
            container.stop();
        }
        container = null;
        subscription = null;
    }

    private void startListeningWithOptions(ChangeStreamOptions csOptions) {
        container = new DefaultMessageListenerContainer(mongoTemplate);
        container.start();

        MessageListener<ChangeStreamDocument<Document>, Document> listener = this::handleMessage;
        var requestOptions = new ChangeStreamRequestOptions(null, COLLECTION, csOptions);
        var request = new ChangeStreamRequest<>(listener, requestOptions);

        subscription = container.register(request, Document.class);

        try {
            subscription.await(Duration.ofSeconds(5));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while awaiting subscription", e);
        }
    }

    private void handleMessage(Message<ChangeStreamDocument<Document>, Document> message) {
        var raw = message.getRaw();
        if (raw == null) return;

        var operationType = raw.getOperationType() != null
                ? raw.getOperationType().getValue() : "unknown";

        var documentKey = extractDocumentKey(raw.getDocumentKey());

        String accountHolder = null;
        Long balance = null;

        var fullDocument = raw.getFullDocument();
        if (fullDocument != null) {
            accountHolder = fullDocument.getString("accountHolder");
            var balanceVal = fullDocument.get("balance");
            if (balanceVal instanceof Number num) {
                balance = num.longValue();
            }
        }

        var notification = AccountNotification.of(operationType, documentKey, accountHolder, balance);
        mongoTemplate.insert(notification);
    }

    private String extractDocumentKey(BsonDocument documentKey) {
        if (documentKey == null) return null;
        var idVal = documentKey.get("_id");
        if (idVal != null && idVal.isObjectId()) {
            return idVal.asObjectId().getValue().toHexString();
        }
        return idVal != null ? idVal.toString() : null;
    }
}
