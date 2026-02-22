package com.mongodb.course.m20.bdd;

import com.mongodb.course.m20.notification.ClaimStatusNotificationListener;
import com.mongodb.course.m20.observability.SlowQueryDetector;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

public class CucumberHooks {

    @Autowired private MongoTemplate mongoTemplate;
    @Autowired private SlowQueryDetector slowQueryDetector;
    @Autowired private ClaimStatusNotificationListener notificationListener;

    @Before
    public void cleanUp() {
        mongoTemplate.remove(new Query(), "m20_claim_events");
        mongoTemplate.remove(new Query(), "m20_snapshots");
        mongoTemplate.remove(new Query(), "m20_claim_dashboard");
        mongoTemplate.remove(new Query(), "m20_claim_statistics");
        mongoTemplate.remove(new Query(), "m20_settlement_saga_logs");
        mongoTemplate.remove(new Query(), "m20_claim_notifications");
        // Drop and recreate policies with schema validation
        if (mongoTemplate.collectionExists("m20_policies")) {
            mongoTemplate.dropCollection("m20_policies");
        }
        new com.mongodb.course.m20.config.SchemaValidationConfig(mongoTemplate).init();
        slowQueryDetector.clear();
        notificationListener.clear();
    }

    @After
    public void tearDown() {
        notificationListener.stop();
    }
}
